package nl.trifox.foxprison.api.events.mines;

import com.hypixel.hytale.event.ICancellable;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.Vector3d;

import java.util.UUID;

public class MineBlockBreakEvent implements IEvent<Void>, ICancellable {
    private final String mineId;

    private final UUID playerUuid;
    private final String worldName;
    private final Vector3i position;
    private boolean cancelled;

    public MineBlockBreakEvent(UUID playerId, String worldName, int x, int y, int z, String mineId) {
        this.playerUuid = playerId;
        this.worldName = worldName;
        this.position  = new Vector3i(x,y,z);
        this.mineId = mineId;
    }

    public String getMineId() { return mineId; }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean b) {
        cancelled = b;
    }

    public Vector3i getPosition() {
        return position;
    }

    public String getWorldName() {
        return worldName;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }
}
