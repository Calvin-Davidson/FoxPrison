package nl.trifox.foxprison.framework.storage.repositories;

import nl.trifox.foxprison.modules.economy.data.PlayerBalanceData;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.UnaryOperator;

public interface PlayerBalanceRepository {

    CompletableFuture<PlayerBalanceData> getOrCreate(UUID playerId);

    CompletableFuture<Boolean> save(UUID playerId, PlayerBalanceData data);

    /**
     * Atomic-ish update for a single player (locks per UUID in implementation).
     */
    CompletableFuture<PlayerBalanceData> update(UUID playerId, UnaryOperator<PlayerBalanceData> mutator);
}