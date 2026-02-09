package nl.trifox.foxprison.api.events.mines;

import com.hypixel.hytale.event.ICancellable;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public final class MineDropsEvent implements IEvent<Void>, ICancellable {

    private final UUID playerUuid;
    private final String worldName;
    private final int x, y, z;
    private final String mineId;

    private final BlockType blockType;
    private final ItemStack toolInHand;

    /** Mutable list: listeners can remove/replace/clear drops */
    private final List<ItemStack> drops;

    private boolean cancelled;

    public MineDropsEvent(
            UUID playerUuid,
            String worldName,
            int x, int y, int z,
            String mineId,
            BlockType blockType,
            ItemStack toolInHand,
            List<ItemStack> drops
    ) {
        this.playerUuid = playerUuid;
        this.worldName = worldName;
        this.x = x; this.y = y; this.z = z;
        this.mineId = mineId;
        this.blockType = blockType;
        this.toolInHand = toolInHand;
        this.drops = drops;
    }

    public UUID getPlayerUuid() { return playerUuid; }
    public String getWorldName() { return worldName; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }
    public String getMineId() { return mineId; }

    public BlockType getBlockType() { return blockType; }
    public ItemStack getToolInHand() { return toolInHand; }
    public List<ItemStack> getDrops() { return drops; }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
}
