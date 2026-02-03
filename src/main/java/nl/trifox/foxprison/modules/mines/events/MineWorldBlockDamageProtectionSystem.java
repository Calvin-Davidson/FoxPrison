package nl.trifox.foxprison.modules.mines.events;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import nl.trifox.foxprison.modules.mines.MineService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MineWorldBlockDamageProtectionSystem extends EntityEventSystem<EntityStore, DamageBlockEvent> {

    private final MineService mineService;

    public MineWorldBlockDamageProtectionSystem(MineService mineService) {
        super(DamageBlockEvent.class);
        this.mineService = mineService;
    }

    @Override
    public void handle(int i, @NotNull ArchetypeChunk<EntityStore> archetypeChunk, @NotNull Store<EntityStore> store, @NotNull CommandBuffer<EntityStore> commandBuffer, @NotNull DamageBlockEvent damageBlockEvent) {
            if (damageBlockEvent.isCancelled()) return;

            var pos = damageBlockEvent.getTargetBlock();

            var player = store.getComponent(archetypeChunk.getReferenceTo(i), Player.getComponentType());
            if (player == null) return;

            var world = player.getWorld();
            if (world == null) return;

            boolean preventBlockBreaking = false;
            for (var mine : mineService.getAllMines()) {
                if (!mine.getWorld().equalsIgnoreCase(world.getName())) continue;

                if (mine.getMineProtectionDefinition().isPreventBlockBreakingOutsideMineInWorld()) {
                    preventBlockBreaking = true;
                }

                if (!mine.getRegion().contains(pos.x, pos.y, pos.z)) continue;

                return;
            }

            if (preventBlockBreaking && player.getGameMode() == GameMode.Adventure) {
                damageBlockEvent.setCancelled(true);
            }
        }


    @Override
    public @Nullable Query<EntityStore> getQuery() {
        return Query.any();
    }
}
