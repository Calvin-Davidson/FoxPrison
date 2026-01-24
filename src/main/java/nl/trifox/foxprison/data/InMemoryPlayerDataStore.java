package nl.trifox.foxprison.data;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryPlayerDataStore implements PlayerDataStore {

    private final ConcurrentHashMap<UUID, PlayerPrisonData> data = new ConcurrentHashMap<>();

    @Override
    public PlayerPrisonData getOrCreate(UUID playerId) {
        return data.computeIfAbsent(playerId, id -> new PlayerPrisonData());
    }
}
