package nl.trifox.foxprison.modules.mines.events;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.EntityUtils;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.DespawnComponent;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import nl.trifox.foxprison.modules.mines.MineService;
import nl.trifox.foxprison.modules.mines.config.AutoResetDefinition;
import nl.trifox.foxprison.modules.mines.data.MineRuntimeState;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.hypixel.hytale.server.core.modules.interaction.BlockHarvestUtils.getDrops;

public class MineItemRemoveSystem extends RefSystem<EntityStore> {

    private final MineService mineService;

    public MineItemRemoveSystem(MineService mineService) {
        this.mineService = mineService;
    }
    @Override
    public void onEntityAdded(@Nonnull Ref<EntityStore> ref, @Nonnull AddReason addReason, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        if (!addReason.equals(AddReason.SPAWN)) return;

        var player = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
        if (player != null) return; // prevent players from being removed

        var transform = commandBuffer.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) return;

        var despawnComponent = commandBuffer.getComponent(ref, DespawnComponent.getComponentType());
        if (despawnComponent == null) return;

        var world = commandBuffer.getExternalData().getWorld();
        // Find mine(s) in that world whose region contains pos
        for (var mine : mineService.getAllMines()) {
            if (!mine.getWorld().equalsIgnoreCase(world.getName())) continue;
            if (!mine.getRegion().contains(transform.getPosition())) continue;

            despawnComponent.setDespawn(Instant.MIN);
            break;
        }
    }

    @Override
    public void onEntityRemove(@NotNull Ref<EntityStore> ref, @NotNull RemoveReason removeReason, @NotNull Store<EntityStore> store, @NotNull CommandBuffer<EntityStore> commandBuffer) {

    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(TransformComponent.getComponentType(), DespawnComponent.getComponentType());
    }
}
