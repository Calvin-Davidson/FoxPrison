package nl.trifox.foxprison.modules.mines.commands.admin;

import com.hypixel.hytale.builtin.buildertools.BuilderToolsPlugin;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
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
import nl.trifox.foxprison.service.MineService;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.concurrent.CompletableFuture;

public class MineCreateCommand extends AbstractAsyncPlayerCommand {

    private final MineService service;

    private final RequiredArg<String> idArg;
    private final RequiredArg<String> nameArg;

    public MineCreateCommand(MineService service) {
        super("create", "Create a mine");
        this.service = service;

        idArg = withRequiredArg("id", "Mine id", ArgTypes.STRING);
        nameArg = withRequiredArg("name", "Display name", ArgTypes.STRING);
    }


    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(@NonNullDecl CommandContext context, @NonNullDecl Store<EntityStore> store, @NonNullDecl Ref<EntityStore> ref, @NonNullDecl PlayerRef playerRef, @NonNullDecl World world) {
        String id = idArg.get(context);
        String name = nameArg.get(context);

        // Must be run by a player
        if (!(context.sender() instanceof Player player)) {
            context.sender().sendMessage(Message.raw("This command must be run by a player."));
            return CompletableFuture.completedFuture(null);
        }

        // Selection provider must exist + be builder tools
        var provider = SelectionManager.getSelectionProvider();
        if (!(provider instanceof BuilderToolsPlugin sp)) {
            context.sender().sendMessage(Message.raw(
                    "Selection tool is not available on this server (builder tools disabled)."
            ));
            return CompletableFuture.completedFuture(null);
        }

        var state = sp.getBuilderState(player, playerRef);
        if (state.getSelection() == null) {
            context.sender().sendMessage(Message.raw(
                    "Nothing selected. Use the Selection Tool to make a selection first."
            ));
            return CompletableFuture.completedFuture(null);
        }

        var sel = state.getSelection();

        int minX = sel.getSelectionMin().x;
        int minY = sel.getSelectionMin().y;
        int minZ = sel.getSelectionMin().z;

        int maxX = sel.getSelectionMax().x;
        int maxY = sel.getSelectionMax().y;
        int maxZ = sel.getSelectionMax().z;

        return service.createMine(
                        id, name, world.getName(),
                        minX, minY, minZ,
                        maxX, maxY, maxZ,
                        playerRef.getTransform()
                )
                .thenAccept(ok -> {
                    context.sender().sendMessage(Message.raw(
                            ok
                                    ? "Mine created: " + id + " (" + name + ") [" +
                                    minX + "," + minY + "," + minZ + "] -> [" +
                                    maxX + "," + maxY + "," + maxZ + "]"
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
