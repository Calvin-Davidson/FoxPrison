package nl.trifox.foxprison.modules.economy;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import nl.trifox.foxprison.FoxPrisonPlugin;
import nl.trifox.foxprison.modules.economy.data.PlayerBalanceData;
import nl.trifox.foxprison.storage.BalanceStorageProvider;
import nl.trifox.foxprison.storage.JsonBalanceStorageProvider;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class EconomyManager {

    // In-memory cache of loaded balances
    private final ConcurrentHashMap<UUID, PlayerBalanceData> cache = new ConcurrentHashMap<>();

    // Per-player locks for atomic operations
    private final ConcurrentHashMap<UUID, ReentrantLock> playerLocks = new ConcurrentHashMap<>();

    // Tracks which players have unsaved changes
    private final Set<UUID> dirtyPlayers = ConcurrentHashMap.newKeySet();

    // Leaderboard cache
    private volatile List<Map.Entry<UUID, PlayerBalanceData>> cachedLeaderboard;
    private volatile long lastLeaderboardRebuild = 0;

    /** Time in milliseconds to cache leaderboard data before rebuilding */
    private static final long LEADERBOARD_CACHE_MS = 2000;

    /** Maximum number of entries to cache in the leaderboard for performance */
    private static final int MAX_LEADERBOARD_CACHE_SIZE = 100;

    /** Number of characters to show when displaying truncated UUIDs */
    private static final int UUID_PREVIEW_LENGTH = 8;

    // Lock eviction timing
    private volatile long lastLockCleanup = System.currentTimeMillis();

    /** Time in milliseconds between lock cleanup cycles (30 minutes) */
    private static final long LOCK_CLEANUP_INTERVAL_MS = 30 * 60 * 1000;

    // Auto-save thread
    private volatile boolean running = true;
    private final Thread saveThread;
    private final HytaleLogger logger;
    private BalanceStorageProvider storage;

    public EconomyManager(FoxPrisonPlugin plugin) throws Exception {
        this.logger = HytaleLogger.getLogger().getSubLogger("Ecotale");

        // Initialize storage provider based on config
        String providerType = FoxPrisonPlugin.getInstance().getCoreConfig().get().getStorageProvider().toLowerCase();
        switch (providerType) {
            case "mysql" -> {
                //this.storage = new MySQLStorageProvider();
                logger.at(Level.INFO).log("Using MySQL storage provider (shared database)");
            }
            case "json" -> {
                this.storage = new JsonBalanceStorageProvider();
                logger.at(Level.INFO).log("Using JSON storage provider");
            }
            default -> {
                throw new Exception("no valid storage provider selected");
            }
        }
        storage.initialize().join();

        // PERF-01: Bulk preload all player data on startup
        bulkPreload();

        // Start auto-save thread
        this.saveThread = new Thread(this::autoSaveLoop, "Ecotale-AutoSave");
        this.saveThread.setDaemon(true);
        this.saveThread.start();

        logger.at(Level.INFO).log("EconomyManager initialized with %s (%d players preloaded)",
                storage.getName(), cache.size());
    }

    // ========== Lock Management ==========

    /**
     * Get or create a lock for a specific player.
     * Locks are cached and reused for the same player.
     */
    private ReentrantLock getLock(UUID playerUuid) {
        return playerLocks.computeIfAbsent(playerUuid, k -> new ReentrantLock());
    }

    // ========== Player Account Management ==========

    /**
     * Ensure a player has an account (load from storage or create new).
     * This is called when a player joins the server.
     */
    public void ensureAccount(@Nonnull UUID playerUuid) {
        cache.computeIfAbsent(playerUuid, uuid -> {
            // Load from storage or create new
            PlayerBalanceData balance = storage.loadPlayer(uuid).join();
            dirtyPlayers.add(uuid); // Mark as dirty to ensure it's saved
            return balance;
        });
    }

    /**
     * Get an account, loading from storage if not in cache.
     */
    private PlayerBalanceData getOrLoadAccount(@Nonnull UUID playerUuid) {
        return cache.computeIfAbsent(playerUuid, uuid ->
                storage.loadPlayer(uuid).join()
        );
    }

    // ========== Balance Operations ==========

    public double getBalance(@Nonnull UUID playerUuid) {
        PlayerBalanceData balance = cache.get(playerUuid);
        return balance != null ? balance.getBalance() : 0.0;
    }

    public PlayerBalanceData getPlayerBalance(@Nonnull UUID playerUuid) {
        return cache.get(playerUuid);
    }

    public boolean hasBalance(@Nonnull UUID playerUuid, double amount) {
        return getBalance(playerUuid) >= amount;
    }

    /**
     * Deposit money into a player's account.
     * Thread-safe with per-player locking.
     * Rejects if would exceed maxBalance.
     * Fires BalanceChangeEvent (cancellable).
     */
    public boolean deposit(@Nonnull UUID playerUuid, double amount, String reason) {
        ReentrantLock lock = getLock(playerUuid);
        lock.lock();
        try {
            PlayerBalanceData balance = getOrLoadAccount(playerUuid);
            if (balance == null) return false;

            double oldBalance = balance.getBalance();
            double newBalance = oldBalance + amount;

            if (balance.deposit(amount, reason)) {
                dirtyPlayers.add(playerUuid);
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Withdraw money from a player's account.
     * Thread-safe with per-player locking.
     * Fires BalanceChangeEvent (cancellable).
     */
    public boolean withdraw(@Nonnull UUID playerUuid, double amount, String reason) {
        ReentrantLock lock = getLock(playerUuid);
        lock.lock();
        try {
            PlayerBalanceData balance = cache.get(playerUuid);
            if (balance == null) return false;

            double oldBalance = balance.getBalance();
            double newBalance = oldBalance - amount;

            if (balance.withdraw(amount, reason)) {
                dirtyPlayers.add(playerUuid);
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Set a player's balance to a specific amount.
     * Thread-safe with per-player locking.
     * Fires BalanceChangeEvent (cancellable).
     */
    public void setBalance(@Nonnull UUID playerUuid, double amount, String reason) {
        ReentrantLock lock = getLock(playerUuid);
        lock.lock();
        try {
            PlayerBalanceData balance = getOrLoadAccount(playerUuid);
            if (balance != null) {
                double oldBalance = balance.getBalance();

                balance.setBalance(amount, reason);
                dirtyPlayers.add(playerUuid);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Transfer money between two players.
     * ATOMIC: Either both operations succeed or neither does.
     * Uses ordered lock acquisition to prevent deadlocks.
     *
     * Security: Fixes SEC-01 (race condition) and DATA-01 (non-atomic transfer)
     */
    public TransferResult transfer(@Nonnull UUID from, @Nonnull UUID to, double amount, String reason) {
        if (from.equals(to)) {
            return TransferResult.SELF_TRANSFER;
        }

        if (amount <= 0) {
            return TransferResult.INVALID_AMOUNT;
        }

        // CRITICAL: Ordered lock acquisition to prevent deadlock
        // Always lock the "smaller" UUID first (consistent ordering)
        UUID first = from.compareTo(to) < 0 ? from : to;
        UUID second = from.compareTo(to) < 0 ? to : from;

        ReentrantLock lock1 = getLock(first);
        ReentrantLock lock2 = getLock(second);

        lock1.lock();
        try {
            lock2.lock();
            try {
                // Get both balances (ensure accounts exist)
                PlayerBalanceData fromBalance = getOrLoadAccount(from);
                PlayerBalanceData toBalance = getOrLoadAccount(to);

                // Check sufficient funds INSIDE the lock
                if (fromBalance == null || !fromBalance.hasBalance(amount)) {
                    return TransferResult.INSUFFICIENT_FUNDS;
                }


                // ATOMIC: Both operations under lock
                fromBalance.withdrawInternal(amount, "Transfer to " + to + ": " + reason);
                toBalance.depositInternal(amount, "Transfer from " + from + ": " + reason);

                // Mark both as dirty
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

    // ========== Bulk Operations ==========

    /**
     * Get all balances (for leaderboards).
     * Returns a snapshot copy to prevent external modification.
     */
    public Map<UUID, PlayerBalanceData> getAllBalances() {
        return new HashMap<>(cache);
    }

    /**
     * Get the number of cached players.
     */
    public int getCachedPlayerCount() {
        return cache.size();
    }

    // ========== Persistence ==========

    /**
     * Mark a player as needing to be saved.
     */
    public void markDirty(@Nonnull UUID playerUuid) {
        dirtyPlayers.add(playerUuid);
    }

    /**
     * Force save all dirty players immediately.
     */
    public void forceSave() {
        saveDirtyPlayers();
    }

    /**
     * Auto-save loop running on background thread.
     * Only saves players that have changes (dirty tracking).
     */
    private void autoSaveLoop() {
        while (running) {
            try {
                Thread.sleep(FoxPrisonPlugin.getInstance().getCoreConfig().get().getAutoSaveInterval() * 1000L);

                if (!dirtyPlayers.isEmpty()) {
                    saveDirtyPlayers();
                }

                // PERF-03: Lock eviction for offline players (every 30 min)
                if (System.currentTimeMillis() - lastLockCleanup > LOCK_CLEANUP_INTERVAL_MS) {
                    cleanupStaleLocks();
                    lastLockCleanup = System.currentTimeMillis();
                }
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    /**
     * Save all dirty players asynchronously.
     */
    private void saveDirtyPlayers() {
        if (dirtyPlayers.isEmpty()) {
            return;
        }

        // Snapshot and clear dirty set
        Set<UUID> toSave = new HashSet<>(dirtyPlayers);
        dirtyPlayers.clear();

        // Build map of dirty players
        Map<UUID, PlayerBalanceData> dirty = new HashMap<>();
        for (UUID uuid : toSave) {
            PlayerBalanceData balance = cache.get(uuid);
            if (balance != null) {
                dirty.put(uuid, balance);
            }
        }

        // Save asynchronously
        storage.saveAll(dirty).exceptionally(e -> {
            logger.at(Level.SEVERE).log("Auto-save failed: %s", e.getMessage());
            // Re-mark as dirty for retry on next cycle
            dirtyPlayers.addAll(toSave);
            return null;
        });
    }

    /**
     * Shutdown the economy manager.
     * Saves all dirty players and stops the auto-save thread.
     */
    public void shutdown() {
        logger.at(Level.INFO).log("EconomyManager shutdown starting... (%d dirty, %d cached)",
                dirtyPlayers.size(), cache.size());

        running = false;
        logger.at(Level.INFO).log("Interrupting auto-save thread...");
        saveThread.interrupt();

        // Save ALL cached players on shutdown (not just dirty) to ensure nothing is lost
        // Use SYNC save to avoid executor issues during server shutdown
        if (!cache.isEmpty()) {
            logger.at(Level.INFO).log("Saving %d player balances...", cache.size());
            try {
                storage.saveAll(cache).get(10, java.util.concurrent.TimeUnit.SECONDS);
                logger.at(Level.INFO).log("Player balances saved successfully");
            } catch (java.util.concurrent.TimeoutException e) {
                logger.at(Level.WARNING).log("Save operation timed out after 10 seconds - data may be lost");
            } catch (Exception e) {
                logger.at(Level.SEVERE).log("Error saving player balances: %s", e.getMessage());
            }
        }

        // Shutdown storage provider
        logger.at(Level.INFO).log("Shutting down storage provider...");
        try {
            storage.shutdown().get(5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            logger.at(Level.WARNING).log("Storage shutdown timed out after 5 seconds");
        } catch (Exception e) {
            logger.at(Level.SEVERE).log("Error during storage shutdown: %s", e.getMessage());
        }

        logger.at(Level.INFO).log("EconomyManager shutdown complete");
    }

    // ========== Storage Access ==========

    /**
     * Get the storage provider.
     */
    public BalanceStorageProvider getStorage() {
        return storage;
    }

    // ========== Performance Optimizations ==========

    /**
     * PERF-01: Bulk preload all player data on startup.
     * Avoids blocking .join() calls during player joins.
     */
    private void bulkPreload() {
        try {
            Map<UUID, PlayerBalanceData> all = storage.loadAll().join();
            cache.putAll(all);
            logger.at(Level.INFO).log("Bulk preloaded %d player balances", all.size());
        } catch (Exception e) {
            logger.at(Level.WARNING).log("Bulk preload failed, will load on-demand: %s", e.getMessage());
        }
    }

    /**
     * PERF-02: Get leaderboard with caching and rate-limit.
     * Avoids sorting entire cache on every request.
     *
     * @param limit Maximum number of entries to return
     * @return Sorted list of top players by balance
     */
    public List<Map.Entry<UUID, PlayerBalanceData>> getLeaderboard(int limit) {
        long now = System.currentTimeMillis();

        // Rebuild if cache is stale or null
        if (cachedLeaderboard == null || now - lastLeaderboardRebuild > LEADERBOARD_CACHE_MS) {
            cachedLeaderboard = cache.entrySet().stream()
                    .sorted((a, b) -> Double.compare(b.getValue().getBalance(), a.getValue().getBalance()))
                    .limit(MAX_LEADERBOARD_CACHE_SIZE)
                    .collect(Collectors.toList());
            lastLeaderboardRebuild = now;
        }

        // Return requested limit from cache
        return cachedLeaderboard.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * PERF-03: Clean up locks for offline players.
     * Prevents unbounded growth of playerLocks map.
     */
    private void cleanupStaleLocks() {
        Set<UUID> onlinePlayers = Universe.get().getPlayers().stream()
                .map(PlayerRef::getUuid)
                .collect(Collectors.toSet());

        int removed = 0;
        for (UUID uuid : new HashSet<>(playerLocks.keySet())) {
            if (!onlinePlayers.contains(uuid)) {
                ReentrantLock lock = playerLocks.get(uuid);
                // Only remove if not currently held
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

    /**
     * Resolve player name for logging purposes.
     * Falls back to truncated UUID if player is offline.
     */
    private String resolvePlayerName(UUID uuid) {
        var player = Universe.get().getPlayer(uuid);
        if (player != null) {
            return player.getUsername();
        }
        return uuid.toString().substring(0, UUID_PREVIEW_LENGTH) + "...";
    }

    public boolean isAvailable() {
        return true;
    }

    // ========== Result Enums ==========

    public enum TransferResult {
        SUCCESS,
        INSUFFICIENT_FUNDS,
        SELF_TRANSFER,
        INVALID_AMOUNT,
        RECIPIENT_MAX_BALANCE
    }
}