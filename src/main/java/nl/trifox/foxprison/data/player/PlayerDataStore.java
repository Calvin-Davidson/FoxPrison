package nl.trifox.foxprison.data.player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface PlayerDataStore {
    CompletableFuture<PlayerPrisonData> getOrCreate(UUID playerId);

    CompletableFuture<Boolean> save(UUID playerId, PlayerPrisonData data);
}
