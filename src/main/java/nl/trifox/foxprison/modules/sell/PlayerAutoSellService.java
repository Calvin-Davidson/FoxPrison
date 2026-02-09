package nl.trifox.foxprison.modules.sell;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerAutoSellService {

    private final Map<UUID, Boolean> enabledByPlayer = new ConcurrentHashMap<>();

    public PlayerAutoSellService() {

    }

    public boolean isEnabled(UUID playerUuid) {
        return enabledByPlayer.getOrDefault(playerUuid, false);
    }

    public void setEnabled(UUID playerUuid, boolean enabled) {
        enabledByPlayer.put(playerUuid, enabled);
    }

    public boolean toggle(UUID playerUuid) {
        boolean next = !isEnabled(playerUuid);
        setEnabled(playerUuid, next);
        return next;
    }

    public void clear(UUID playerUuid) {
        enabledByPlayer.remove(playerUuid);
    }
}
