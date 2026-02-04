package nl.trifox.foxprison.modules.mines.events;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import nl.trifox.foxprison.api.events.mines.MineBlockBreakEvent;
import nl.trifox.foxprison.modules.mines.config.AutoResetDefinition;
import nl.trifox.foxprison.modules.mines.data.MineRuntimeState;
import nl.trifox.foxprison.modules.mines.MineService;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import static com.hypixel.hytale.server.core.modules.interaction.BlockHarvestUtils.getDrops;


public class MineBlockBreakEventSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {

    private final MineService mineService;
    private final IEventDispatcher<MineBlockBreakEvent, MineBlockBreakEvent> mineBLockBreakEventDispatcher;

    public MineBlockBreakEventSystem(MineService mineService) {
        super(BreakBlockEvent.class);
        this.mineService = mineService;
        mineBLockBreakEventDispatcher = HytaleServer.get().getEventBus().dispatchFor(MineBlockBreakEvent.class);
    }

    @Override
    public void handle(int i, @NonNullDecl ArchetypeChunk<EntityStore> archetypeChunk, @NonNullDecl Store<EntityStore> store, @NonNullDecl CommandBuffer<EntityStore> commandBuffer, @NonNullDecl BreakBlockEvent breakBlockEvent) {
        if (breakBlockEvent.isCancelled()) return;

        var pos = breakBlockEvent.getTargetBlock();

        var player = store.getComponent(archetypeChunk.getReferenceTo(i), Player.getComponentType());
        if (player == null || player.getReference() == null) return;

        var uuidComponent = store.getComponent(player.getReference(), UUIDComponent.getComponentType());
        if (uuidComponent == null) return;

        var world = player.getWorld();
        if (world == null) return;

        // Find mine(s) in that world whose region contains pos
        for (var mine : mineService.getAllMines()) {
            if (!mine.getWorld().equalsIgnoreCase(world.getName())) continue;
            if (!mine.getRegion().contains(pos.x, pos.y, pos.z)) continue;

            var event = new MineBlockBreakEvent(uuidComponent.getUuid(), world.getName(), pos.x, pos.y, pos.z, mine.getId());
            var result = mineBLockBreakEventDispatcher.dispatch(event);
            if (result.isCancelled()) {
                event.setCancelled(true);
                return;
            }

            BlockType blockType = world.getBlockType(pos.x, pos.y, pos.z);
            if (blockType == null) return;

            var inv = player.getInventory();
            if (inv != null) {
                var drops = getDrops(blockType, 1, null, blockType.getGathering().getBreaking().getDropListId());

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
        return Query.or(PlayerRef.getComponentType(), ItemComponent.getComponentType(), UUIDComponent.getComponentType());
    }
}
