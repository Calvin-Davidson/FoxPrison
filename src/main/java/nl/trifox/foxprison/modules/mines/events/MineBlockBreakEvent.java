package nl.trifox.foxprison.modules.mines.events;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockBreakingDropType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockGathering;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.entity.item.PickupItemComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.SetBlockSettings;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import nl.trifox.foxprison.modules.mines.config.AutoResetDefinition;
import nl.trifox.foxprison.modules.mines.data.MineRuntimeState;
import nl.trifox.foxprison.modules.mines.MineService;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import static com.hypixel.hytale.server.core.modules.interaction.BlockHarvestUtils.getDrops;


public class MineBlockBreakEvent extends EntityEventSystem<EntityStore, BreakBlockEvent> {


    private final MineService mineService;

    public MineBlockBreakEvent(MineService mineService) {
        super(BreakBlockEvent.class);
        this.mineService = mineService;
    }

    @Override
    public void handle(int i, @NonNullDecl ArchetypeChunk<EntityStore> archetypeChunk, @NonNullDecl Store<EntityStore> store, @NonNullDecl CommandBuffer<EntityStore> commandBuffer, @NonNullDecl BreakBlockEvent breakBlockEvent) {
        if (breakBlockEvent.isCancelled()) return;

        var pos = breakBlockEvent.getTargetBlock();

        var player = store.getComponent(archetypeChunk.getReferenceTo(i), Player.getComponentType());
        if (player == null) return;

        var world = player.getWorld();
        if (world == null) return;

        // Find mine(s) in that world whose region contains pos
        for (var mine : mineService.getAllMines()) {
            if (!mine.getWorld().equalsIgnoreCase(world.getName())) continue;
            if (!mine.getRegion().contains(pos.x, pos.y, pos.z)) continue;

            BlockType blockType = world.getBlockType(pos.x, pos.y, pos.z);
            if (blockType == null) return;
            world.breakBlock(pos.getX(), pos.getY(), pos.getZ(), SetBlockSettings.NO_DROP_ITEMS);

            var inv = player.getInventory();
            if (inv != null) {
                var drops = getDrops(blockType, 1, breakBlockEvent.getBlockType().getId(), null);
                if (inv.getCombinedBackpackStorageHotbar().canAddItemStacks(drops)) {
                    inv.getCombinedBackpackStorageHotbar().addItemStacks(drops);
                }
            }

            AutoResetDefinition ar = mine.getAutoReset();
            if (ar == null || !ar.isEnabled()) return;

            int threshold = ar.getBlocksBrokenThreshold();
            if (threshold <= 0) return;

            MineRuntimeState st = mineService.getState(mine.getId());
            int broken = st.brokenBlocks.incrementAndGet();

            if (broken >= threshold) {
                mineService.triggerResetIfAllowed(mine.getId(), System.currentTimeMillis(), "blocks-broken");
            }
            break;
        }
    }

    @NullableDecl
    @Override
    public Query<EntityStore> getQuery() {
        return Query.or(PlayerRef.getComponentType(), ItemComponent.getComponentType());
    }
}
