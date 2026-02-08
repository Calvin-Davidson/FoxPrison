package nl.trifox.foxprison.modules.mines.commands.admin;

import com.hypixel.hytale.builtin.buildertools.BuilderToolsPlugin;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.prefab.selection.SelectionManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import nl.trifox.foxprison.modules.mines.MineService;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.concurrent.CompletableFuture;

public class MineSetSpawn extends AbstractAsyncPlayerCommand {

    private final MineService service;

    private final RequiredArg<String> idArg;

    public MineSetSpawn(MineService service) {
        super("setspawn", "set's the mine spawn to the current player position and rotation");
        this.service = service;
        requirePermission("foxprison.mine.command.admin.setspawn");

        idArg = withRequiredArg("id", "Mine id", ArgTypes.STRING);
    }


    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(@NonNullDecl CommandContext context, @NonNullDecl Store<EntityStore> store, @NonNullDecl Ref<EntityStore> ref, @NonNullDecl PlayerRef playerRef, @NonNullDecl World world) {
        String id = idArg.get(context);

        // Must be run by a player
        if (!(context.sender() instanceof Player player)) {
            context.sender().sendMessage(Message.raw("This command must be run by a player."));
            return CompletableFuture.completedFuture(null);
        }

        var mine = service.getMine(id);
        if (mine.isEmpty()) {
            context.sender().sendMessage(Message.raw("Mine not found"));
            return CompletableFuture.completedFuture(null);
        }

        var transform = playerRef.getTransform();
        service.setSpawnPoint(id, transform);
        mine.get().setSpawn(transform);


        return service.setSpawnPoint(id, playerRef.getTransform())
                .thenAccept(ok -> {
                    context.sender().sendMessage(Message.raw(
                            ok
                                    ? "Mine spawnpoint changed"
                                    : "Failed to create mine (duplicate id or invalid id)."
                    ));
                })
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    context.sender().sendMessage(Message.raw("Failed to create mine due to an internal error."));
                    return null;
                });
    }
}
