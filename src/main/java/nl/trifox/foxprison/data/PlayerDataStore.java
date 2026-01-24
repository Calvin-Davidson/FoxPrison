package nl.trifox.foxprison.data;

import java.util.UUID;

public interface PlayerDataStore {
    PlayerPrisonData getOrCreate(UUID playerId);
}
