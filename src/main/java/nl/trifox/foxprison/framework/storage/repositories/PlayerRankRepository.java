package nl.trifox.foxprison.framework.storage.repositories;

import nl.trifox.foxprison.modules.ranks.data.PlayerRankData;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.UnaryOperator;

public interface PlayerRankRepository {
    CompletableFuture<PlayerRankData> getOrCreate(UUID playerId);

    CompletableFuture<Boolean> save(UUID playerId, PlayerRankData data);

    CompletableFuture<PlayerRankData> update(UUID playerId, UnaryOperator<PlayerRankData> mutator);
}
