package nl.trifox.foxprison.storage;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface StorageProvider<T> {
    /**
     * Initialize the storage system.
     * Called once on plugin startup.
     */
    CompletableFuture<Void> initialize();

    /**
     * Load a player's balance from storage.
     * Creates a new account with starting balance if not exists.
     *
     * @param playerUuid The player's UUID
     * @return The player's balance data
     */
    CompletableFuture<T> loadPlayer(@Nonnull UUID playerUuid);

    /**
     * Save a single player's balance to storage.
     * Implements atomic write with backup rotation.
     *
     * @param playerUuid The player's UUID
     * @param balance The balance data to save
     */
    CompletableFuture<Void> savePlayer(@Nonnull UUID playerUuid, @Nonnull T balance);

    /**
     * Batch save multiple players' balances.
     * More efficient than individual saves for auto-save.
     *
     * @param dirtyPlayers Map of UUID to T for changed players
     */
    CompletableFuture<Void> saveAll(@Nonnull Map<UUID, T> dirtyPlayers);

    /**
     * Load all player balances.
     * Used for leaderboards and startup migration.
     *
     * @return Map of all player UUIDs to their balances
     */
    CompletableFuture<Map<UUID, T>> loadAll();

    /**
     * Check if a player has saved data.
     *
     * @param playerUuid The player's UUID
     * @return true if player data exists in storage
     */
    CompletableFuture<Boolean> playerExists(@Nonnull UUID playerUuid);

    /**
     * Delete a player's balance data.
     * Used for account resets or GDPR compliance.
     *
     * @param playerUuid The player's UUID
     */
    CompletableFuture<Void> deletePlayer(@Nonnull UUID playerUuid);

    /**
     * Shutdown and cleanup resources.
     * Ensures all pending writes are flushed.
     */
    CompletableFuture<Void> shutdown();

    /**
     * Get the provider's display name for logging.
     */
    String getName();

    /**
     * Get the number of players with saved data.
     */
    int getPlayerCount();
}
