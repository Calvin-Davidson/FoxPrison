package nl.trifox.foxprison.modules.economy.manager;

import com.google.gson.Gson;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import nl.trifox.foxprison.FoxPrisonPlugin;
import nl.trifox.foxprison.framework.storage.StorageProvider;
import nl.trifox.foxprison.framework.storage.repositories.PlayerBalanceRepository;
import nl.trifox.foxprison.modules.economy.EconomyManager;
import nl.trifox.foxprison.modules.economy.config.CurrencyDefinition;
import nl.trifox.foxprison.modules.economy.config.EconomyConfig;
import nl.trifox.foxprison.modules.economy.data.PlayerBalanceData;
import nl.trifox.foxprison.modules.economy.enums.TransferResult;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class FoxEconomyManager implements EconomyManager {

    private final ConcurrentHashMap<UUID, PlayerBalanceData> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, ReentrantLock> playerLocks = new ConcurrentHashMap<>();
    private final Set<UUID> dirtyPlayers = ConcurrentHashMap.newKeySet();

    // Leaderboard cache
    private volatile List<Map.Entry<UUID, PlayerBalanceData>> cachedLeaderboard;
    private volatile long lastLeaderboardRebuild = 0;

    private static final long LEADERBOARD_CACHE_MS = 2000;
    private static final int MAX_LEADERBOARD_CACHE_SIZE = 100;
    private static final int UUID_PREVIEW_LENGTH = 8;

    private volatile long lastLockCleanup = System.currentTimeMillis();
    private static final long LOCK_CLEANUP_INTERVAL_MS = 30 * 60 * 1000;

    private volatile boolean running = true;
    private final Thread saveThread;
    private final AtomicBoolean saving = new AtomicBoolean(false);

    private final HytaleLogger logger;
    private final StorageProvider storage;

    // Used ONLY to deep-copy snapshots for saving (prevents torn writes).
    private final Gson snapshotGson = new Gson();
    private final EconomyConfig economyConfig;

    public FoxEconomyManager(FoxPrisonPlugin plugin, StorageProvider storage) {
        this.logger = HytaleLogger.getLogger().getSubLogger("Ecotale");
        this.storage = storage;
        this.economyConfig = plugin.getEconomyConfig().get();

        this.saveThread = new Thread(this::autoSaveLoop, "Ecotale-AutoSave");
        this.saveThread.setDaemon(true);
        this.saveThread.start();

        logger.at(Level.INFO).log("EconomyManager initialized with %s (%d players cached)",
                storage.getName(), cache.size());
    }

    // ========== Lock Management ==========

    private ReentrantLock getLock(UUID playerUuid) {
        return playerLocks.computeIfAbsent(playerUuid, k -> new ReentrantLock());
    }

    private static boolean isValidDelta(double amount) {
        return amount > 0.0 && Double.isFinite(amount);
    }

    private static boolean isValidBalance(double amount) {
        return amount >= 0.0 && Double.isFinite(amount);
    }

    private PlayerBalanceData deepCopy(PlayerBalanceData data) {
        // Snapshot by JSON roundtrip (safe and simple). If you add a copy() method later, switch to that.
        return snapshotGson.fromJson(snapshotGson.toJson(data), PlayerBalanceData.class);
    }

    // ========== Currency Helpers ==========

    private String defaultCurrencyId() {
        return CurrencyDefinition.normalize(economyConfig.getDefaultCurrencyId());
    }

    /**
     * Returns a normalized currency id if allowed by config, otherwise null.
     * Prevents unknown currencies from creating wallets by accident.
     */
    private String resolveCurrencyOrNull(String currencyId) {
        String resolved = (currencyId == null || currencyId.isBlank())
                ? defaultCurrencyId()
                : CurrencyDefinition.normalize(currencyId);

        Set<String> allowed = economyConfig.getCurrencyIds();

        // If config is empty/misconfigured, only allow default currency.
        if (allowed == null || allowed.isEmpty()) {
            return resolved.equalsIgnoreCase(defaultCurrencyId()) ? defaultCurrencyId() : null;
        }

        // Normalize allowed set lookup (in case config stored mixed-case)
        if (allowed.contains(resolved)) return resolved;

        // Fallback: case-insensitive check (keeps behavior sane if config isn't normalized)
        for (String a : allowed) {
            if (a != null && a.equalsIgnoreCase(resolved)) return CurrencyDefinition.normalize(a);
        }

        return null;
    }

    private boolean hasWallet(PlayerBalanceData data, String currencyId) {
        try {
            var wallets = data.getWallets();
            if (wallets == null || wallets.length == 0) return false;

            String wanted = CurrencyDefinition.normalize(currencyId);
            for (var w : wallets) {
                if (w == null) continue;
                String got = CurrencyDefinition.normalize(w.getCurrencyId());
                if (got.equalsIgnoreCase(wanted)) return true;
            }
            return false;
        } catch (Throwable t) {
            // If getWallets doesn't exist in your PlayerBalanceData version,
            // this will fail at compile time anyway. Keeping runtime-safe here.
            return false;
        }
    }

    private void ensureWalletsIfMissing(UUID playerUuid, PlayerBalanceData data) {
        if (data == null) return;

        // Make sure uuid is set (useful for older saves)
        try {
            data.setPlayerUuidIfMissing(playerUuid);
        } catch (Throwable ignored) {}

        Set<String> ids = economyConfig.getCurrencyIds();
        if (ids == null || ids.isEmpty()) {
            ids = Set.of(defaultCurrencyId());
        }

        boolean missing = false;
        for (String id : ids) {
            if (id == null) continue;
            if (!hasWallet(data, id)) {
                missing = true;
                break;
            }
        }

        if (missing) {
            data.ensureWallets(economyConfig);
            dirtyPlayers.add(playerUuid);
        }
    }

    // ========== Account Management ==========

    @Override
    public void ensureAccount(@Nonnull UUID playerUuid) {
        PlayerBalanceData data = getOrLoadAccount(playerUuid);
        if (data != null) {
            ensureWalletsIfMissing(playerUuid, data);
        }
        // IMPORTANT: don't mark dirty on load unless we actually had to add wallets.
    }

    @Override
    public String getDefaultCurrencyID() {
        return defaultCurrencyId();
    }

    private PlayerBalanceData getOrLoadAccount(@Nonnull UUID playerUuid) {
        PlayerBalanceData data = cache.computeIfAbsent(playerUuid, uuid -> storage.balances().getOrCreate(uuid).join());
        if (data != null) {
            ensureWalletsIfMissing(playerUuid, data);
        }
        return data;
    }

    // ========== Balance Ops (Default currency) ==========

    @Override
    public double getBalance(@Nonnull UUID playerUuid) {
        return getBalance(playerUuid, defaultCurrencyId());
    }

    @Override
    public boolean hasBalance(@Nonnull UUID playerUuid, double amount) {
        return hasBalance(playerUuid, amount, defaultCurrencyId());
    }

    @Override
    public boolean deposit(@Nonnull UUID playerUuid, double amount, String reason) {
        return deposit(playerUuid, amount, reason, defaultCurrencyId());
    }

    @Override
    public boolean withdraw(@Nonnull UUID playerUuid, double amount, String reason) {
        return withdraw(playerUuid, amount, reason, defaultCurrencyId());
    }

    @Override
    public void setBalance(@Nonnull UUID playerUuid, double amount, String reason) {
        setBalance(playerUuid, amount, reason, defaultCurrencyId());
    }

    @Override
    public TransferResult transfer(@Nonnull UUID from, @Nonnull UUID to, double amount, String reason) {
        return transfer(from, to, amount, reason, defaultCurrencyId());
    }

    // ========== Balance Ops (Multi-currency) ==========

    @Override
    public double getBalance(@NotNull UUID playerUuid, String currencyId) {
        String currency = resolveCurrencyOrNull(currencyId);
        if (currency == null) return 0.0;

        PlayerBalanceData balance = getOrLoadAccount(playerUuid);
        return balance != null ? balance.getBalance(currency) : 0.0;
    }

    @Override
    public boolean hasBalance(@NotNull UUID playerUuid, double amount, String currencyId) {
        if (!Double.isFinite(amount)) return false;
        if (amount <= 0) return true;

        String currency = resolveCurrencyOrNull(currencyId);
        if (currency == null) return false;

        PlayerBalanceData balance = getOrLoadAccount(playerUuid);
        return balance != null && balance.getBalance(currency) >= amount; // ✅ >=
    }

    @Override
    public boolean deposit(@NotNull UUID playerUuid, double amount, String reason, String currencyId) {
        if (!isValidDelta(amount)) return false;

        String currency = resolveCurrencyOrNull(currencyId);
        if (currency == null) return false;

        ReentrantLock lock = getLock(playerUuid);
        lock.lock();
        try {
            PlayerBalanceData balance = getOrLoadAccount(playerUuid);
            if (balance == null) return false;

            boolean ok = balance.deposit(currency, amount, (reason == null ? "" : reason));
            if (ok) dirtyPlayers.add(playerUuid);
            return ok;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean withdraw(@NotNull UUID playerUuid, double amount, String reason, String currencyId) {
        if (!isValidDelta(amount)) return false;

        String currency = resolveCurrencyOrNull(currencyId);
        if (currency == null) return false;

        ReentrantLock lock = getLock(playerUuid);
        lock.lock();
        try {
            PlayerBalanceData balance = getOrLoadAccount(playerUuid);
            if (balance == null) return false;

            boolean ok = balance.withdraw(currency, amount, (reason == null ? "" : reason));
            if (ok) dirtyPlayers.add(playerUuid);
            return ok;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void setBalance(@NotNull UUID playerUuid, double amount, String reason, String currencyId) {
        if (!isValidBalance(amount)) return;

        String currency = resolveCurrencyOrNull(currencyId);
        if (currency == null) return;

        ReentrantLock lock = getLock(playerUuid);
        lock.lock();
        try {
            PlayerBalanceData balance = getOrLoadAccount(playerUuid);
            if (balance == null) return;

            balance.setBalance(currency, amount, (reason == null ? "" : reason));
            dirtyPlayers.add(playerUuid);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public TransferResult transfer(@NotNull UUID from, @NotNull UUID to, double amount, String reason, String currencyId) {
        if (from.equals(to)) return TransferResult.SELF_TRANSFER;
        if (!isValidDelta(amount)) return TransferResult.INVALID_AMOUNT;

        String currency = resolveCurrencyOrNull(currencyId);
        if (currency == null) return TransferResult.FAILED;

        // Deadlock-safe lock ordering
        UUID first = from.compareTo(to) < 0 ? from : to;
        UUID second = from.compareTo(to) < 0 ? to : from;

        ReentrantLock lock1 = getLock(first);
        ReentrantLock lock2 = getLock(second);

        lock1.lock();
        try {
            lock2.lock();
            try {
                PlayerBalanceData fromBalance = getOrLoadAccount(from);
                PlayerBalanceData toBalance = getOrLoadAccount(to);

                if (fromBalance == null || toBalance == null) {
                    return TransferResult.FAILED;
                }

                if (!fromBalance.hasBalance(currency, amount)) {
                    return TransferResult.INSUFFICIENT_FUNDS;
                }

                String r = (reason == null ? "" : reason);

                // Atomic under both locks
                fromBalance.withdrawUnsafe(currency, amount, "Transfer to " + to + ": " + r);
                toBalance.depositUnsafe(currency, amount, "Transfer from " + from + ": " + r);

                dirtyPlayers.add(from);
                dirtyPlayers.add(to);

                return TransferResult.SUCCESS;
            } finally {
                lock2.unlock();
            }
        } finally {
            lock1.unlock();
        }
    }

    // ========== Persistence ==========

    public void markDirty(@Nonnull UUID playerUuid) {
        dirtyPlayers.add(playerUuid);
    }

    @Override
    public void forceSave() {
        saveDirtyPlayersAsync().join();
    }

    private void autoSaveLoop() {
        while (running) {
            try {
                long intervalMs = FoxPrisonPlugin.getInstance()
                        .getCoreConfig().get().getAutoSaveInterval() * 1000L;

                Thread.sleep(intervalMs);

                if (!dirtyPlayers.isEmpty() && saving.compareAndSet(false, true)) {
                    saveDirtyPlayersAsync()
                            .whenComplete((v, ex) -> saving.set(false));
                }

                if (System.currentTimeMillis() - lastLockCleanup > LOCK_CLEANUP_INTERVAL_MS) {
                    cleanupStaleLocks();
                    lastLockCleanup = System.currentTimeMillis();
                }
            } catch (InterruptedException e) {
                break;
            } catch (Throwable t) {
                logger.atSevere().log("Auto-save loop error: " + t.getMessage(), t);
            }
        }
    }

    private CompletableFuture<Void> saveDirtyPlayersAsync() {
        List<UUID> candidates = new ArrayList<>(dirtyPlayers);
        if (candidates.isEmpty()) return CompletableFuture.completedFuture(null);

        Map<UUID, PlayerBalanceData> snapshots = new HashMap<>();

        for (UUID uuid : candidates) {
            ReentrantLock lock = getLock(uuid);
            lock.lock();
            try {
                if (!dirtyPlayers.contains(uuid)) continue;

                PlayerBalanceData live = cache.get(uuid);
                if (live == null) {
                    dirtyPlayers.remove(uuid);
                    continue;
                }

                PlayerBalanceData snap;
                try {
                    snap = deepCopy(live);
                } catch (Exception ex) {
                    // keep dirty so it retries later
                    logger.atSevere().log("Snapshot failed for " + uuid + ": " + ex.getMessage(), ex);
                    continue;
                }

                // Clear dirty after snapshot
                dirtyPlayers.remove(uuid);

                snapshots.put(uuid, snap);
            } finally {
                lock.unlock();
            }
        }

        if (snapshots.isEmpty()) return CompletableFuture.completedFuture(null);

        var failed = ConcurrentHashMap.<UUID>newKeySet();

        CompletableFuture<?>[] tasks = snapshots.entrySet().stream()
                .map(entry -> {
                    UUID uuid = entry.getKey();
                    PlayerBalanceData snap = entry.getValue();

                    return storage.balances()
                            .save(uuid, snap)
                            .handle((ok, ex) -> {
                                if (ex != null) {
                                    failed.add(uuid);
                                    logger.atSevere().log("Auto-save failed for " + uuid + ": " + ex.getMessage(), ex);
                                } else if (!Boolean.TRUE.equals(ok)) {
                                    failed.add(uuid);
                                    logger.atSevere().log("Auto-save returned false for " + uuid);
                                }
                                return null;
                            });
                })
                .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(tasks)
                .whenComplete((v, ex) -> {
                    // Re-mark only failed players
                    if (!failed.isEmpty()) {
                        dirtyPlayers.addAll(failed);
                    }
                    // If batch itself exploded, re-mark all snapshots
                    if (ex != null) {
                        logger.atSevere().log("Auto-save batch failed: " + ex.getMessage(), ex);
                        dirtyPlayers.addAll(snapshots.keySet());
                    }
                });
    }

    // ========== Shutdown ==========

    @Override
    public void shutdown() {
        logger.at(Level.INFO).log("EconomyManager shutdown starting... (%d dirty, %d cached)",
                dirtyPlayers.size(), cache.size());

        running = false;
        saveThread.interrupt();

        // Save ALL cached players, and WAIT
        if (!cache.isEmpty()) {
            dirtyPlayers.addAll(cache.keySet());
            try {
                saveDirtyPlayersAsync().join();
                logger.at(Level.INFO).log("Player balances saved successfully");
            } catch (Exception e) {
                logger.at(Level.SEVERE).log("Error saving player balances: %s", e.getMessage());
            }
        }

        logger.at(Level.INFO).log("EconomyManager shutdown complete");
    }

    public PlayerBalanceRepository getStorage() {
        return storage.balances();
    }

    // ========== Leaderboard / Perf ==========

    public List<Map.Entry<UUID, PlayerBalanceData>> getLeaderboard(int limit) {
        long now = System.currentTimeMillis();
        if (cachedLeaderboard == null || now - lastLeaderboardRebuild > LEADERBOARD_CACHE_MS) {
            String currency = defaultCurrencyId();
            cachedLeaderboard = cache.entrySet().stream()
                    .sorted((a, b) -> Double.compare(
                            b.getValue().getBalance(currency),
                            a.getValue().getBalance(currency)
                    ))
                    .limit(MAX_LEADERBOARD_CACHE_SIZE)
                    .collect(Collectors.toList());
            lastLeaderboardRebuild = now;
        }
        return cachedLeaderboard.stream().limit(limit).collect(Collectors.toList());
    }

    private void cleanupStaleLocks() {
        Set<UUID> onlinePlayers = Universe.get().getPlayers().stream()
                .map(PlayerRef::getUuid)
                .collect(Collectors.toSet());

        int removed = 0;
        for (UUID uuid : new HashSet<>(playerLocks.keySet())) {
            if (!onlinePlayers.contains(uuid)) {
                ReentrantLock lock = playerLocks.get(uuid);
                if (lock != null && !lock.isLocked()) {
                    playerLocks.remove(uuid);
                    removed++;
                }
            }
        }

        if (removed > 0) {
            logger.at(Level.FINE).log("Cleaned up %d stale player locks", removed);
        }
    }

    private String resolvePlayerName(UUID uuid) {
        var player = Universe.get().getPlayer(uuid);
        if (player != null) return player.getUsername();
        return uuid.toString().substring(0, UUID_PREVIEW_LENGTH) + "...";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
