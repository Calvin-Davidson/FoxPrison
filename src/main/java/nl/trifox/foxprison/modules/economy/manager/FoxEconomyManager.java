package nl.trifox.foxprison.modules.economy.manager;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import nl.trifox.foxprison.FoxPrisonPlugin;
import nl.trifox.foxprison.framework.storage.StorageProvider;
import nl.trifox.foxprison.framework.storage.repositories.PlayerBalanceRepository;
import nl.trifox.foxprison.modules.economy.EconomyManager;
import nl.trifox.foxprison.modules.economy.data.PlayerBalanceData;
import nl.trifox.foxprison.modules.economy.enums.TransferResult;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.stream.Collectors;


import com.google.gson.Gson;
import com.hypixel.hytale.logger.HytaleLogger;

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

    public FoxEconomyManager(FoxPrisonPlugin plugin, StorageProvider storage) {
        this.logger = HytaleLogger.getLogger().getSubLogger("Ecotale");
        this.storage = storage;

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

    // ========== Account Management ==========

    public void ensureAccount(@Nonnull UUID playerUuid) {
        cache.computeIfAbsent(playerUuid, uuid -> storage.balances().getOrCreate(uuid).join());
        // IMPORTANT: don't mark dirty on load. Only mark dirty on actual mutation.
    }

    @Override
    public String getDefaultCurrencyID() {
        return "coins";
    }

    private PlayerBalanceData getOrLoadAccount(@Nonnull UUID playerUuid) {
        return cache.computeIfAbsent(playerUuid, uuid -> storage.balances().getOrCreate(uuid).join());
    }

    // ========== Balance Ops ==========

    public double getBalance(@Nonnull UUID playerUuid) {
        PlayerBalanceData balance = cache.get(playerUuid);
        return balance != null ? balance.getBalance() : 0.0;
    }

    public boolean hasBalance(@Nonnull UUID playerUuid, double amount) {
        return getBalance(playerUuid) >= amount;
    }

    public boolean deposit(@Nonnull UUID playerUuid, double amount, String reason) {
        if (!isValidDelta(amount)) return false;

        ReentrantLock lock = getLock(playerUuid);
        lock.lock();
        try {
            PlayerBalanceData balance = getOrLoadAccount(playerUuid);
            if (balance == null) return false;

            boolean ok = balance.deposit(amount, reason);
            if (ok) dirtyPlayers.add(playerUuid);
            return ok;
        } finally {
            lock.unlock();
        }
    }

    public boolean withdraw(@Nonnull UUID playerUuid, double amount, String reason) {
        if (!isValidDelta(amount)) return false;

        ReentrantLock lock = getLock(playerUuid);
        lock.lock();
        try {
            // FIX: load if missing (previously cache.get() caused false negatives)
            PlayerBalanceData balance = getOrLoadAccount(playerUuid);
            if (balance == null) return false;

            boolean ok = balance.withdraw(amount, reason);
            if (ok) dirtyPlayers.add(playerUuid);
            return ok;
        } finally {
            lock.unlock();
        }
    }

    public void setBalance(@Nonnull UUID playerUuid, double amount, String reason) {
        if (!isValidBalance(amount)) return;

        ReentrantLock lock = getLock(playerUuid);
        lock.lock();
        try {
            PlayerBalanceData balance = getOrLoadAccount(playerUuid);
            if (balance == null) return;

            balance.setBalance(amount, reason);
            dirtyPlayers.add(playerUuid);
        } finally {
            lock.unlock();
        }
    }

    public TransferResult transfer(@Nonnull UUID from, @Nonnull UUID to, double amount, String reason) {
        if (from.equals(to)) return TransferResult.SELF_TRANSFER;
        if (!isValidDelta(amount)) return TransferResult.INVALID_AMOUNT;

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

                if (!fromBalance.hasBalance(amount)) {
                    return TransferResult.INSUFFICIENT_FUNDS;
                }

                // Atomic under both locks
                fromBalance.withdrawUnsafe(amount, "Transfer to " + to + ": " + reason);
                toBalance.depositUnsafe(amount, "Transfer from " + from + ": " + reason);

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

    public void forceSave() {
        // Save current dirty set and wait for completion
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
        // We remove dirty marks per-player under the same lock used for mutation,
        // and snapshot data while locked (prevents torn writes).
        List<UUID> candidates = new ArrayList<>(dirtyPlayers);
        if (candidates.isEmpty()) return CompletableFuture.completedFuture(null);

        Map<UUID, PlayerBalanceData> snapshots = new HashMap<>();
        Set<UUID> removed = new HashSet<>();

        for (UUID uuid : candidates) {
            ReentrantLock lock = getLock(uuid);
            lock.lock();
            try {
                if (!dirtyPlayers.contains(uuid)) continue; // already handled
                PlayerBalanceData live = cache.get(uuid);
                if (live == null) {
                    // Nothing to save; just drop dirty flag
                    dirtyPlayers.remove(uuid);
                    removed.add(uuid);
                    continue;
                }

                PlayerBalanceData snap;
                try {
                    snap = deepCopy(live);
                } catch (Exception ex) {
                    // If snapshot fails, keep dirty so it retries later
                    logger.atSevere().log("Snapshot failed for " + uuid + ": " + ex.getMessage(), ex);
                    continue;
                }

                // Now that we have a consistent snapshot, clear dirty for this player
                dirtyPlayers.remove(uuid);
                removed.add(uuid);

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
                            .save(uuid, snap) // CompletableFuture<Boolean>
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
            cachedLeaderboard = cache.entrySet().stream()
                    .sorted((a, b) -> Double.compare(b.getValue().getBalance(), a.getValue().getBalance()))
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

    public boolean isAvailable() {
        return true;
    }
}
