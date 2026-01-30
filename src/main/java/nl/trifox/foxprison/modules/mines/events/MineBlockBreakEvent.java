package nl.trifox.foxprison.modules.mines.events;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import nl.trifox.foxprison.modules.mines.config.AutoResetDefinition;
import nl.trifox.foxprison.modules.mines.data.MineRuntimeState;
import nl.trifox.foxprison.modules.mines.MineService;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

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

        boolean preventBlockBreaking = false;
        boolean isBlockInMine = false;
        // Find mine(s) in that world whose region contains pos
        for (var mine : mineService.getAllMines()) {
            if (!mine.getWorld().equalsIgnoreCase(world.getName())) continue;

            if (mine.isPreventBlockBreakingOutsideMineInWorld()) {
                preventBlockBreaking = true;
            }

            if (!mine.getRegion().contains(pos.x, pos.y, pos.z)) continue;

            isBlockInMine = true;

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

        if (preventBlockBreaking && !isBlockInMine && player.getGameMode() == GameMode.Adventure) {
            breakBlockEvent.setCancelled(true);
        }
    }

    @NullableDecl
    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }
}
