package nl.trifox.foxprison.modules.mines.commands.admin;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncPlayerCommand;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import nl.trifox.foxprison.modules.mines.MineService;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.concurrent.CompletableFuture;

public class MineTpCommand extends AbstractAsyncPlayerCommand {

    private final MineService service;
    private final RequiredArg<String> mineArg;

    public MineTpCommand(MineService service) {
        super("minetp", "Teleport to a mine");
        this.service = service;
        this.mineArg = withRequiredArg("mineId", "Mine id", ArgTypes.STRING);
        requirePermission("foxprison.mine.command.admin.minetp");
    }


    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(@NonNullDecl CommandContext context, @NonNullDecl Store<EntityStore> store, @NonNullDecl Ref<EntityStore> ref, @NonNullDecl PlayerRef playerRef, @NonNullDecl World world) {
        var mine = service.getMine(mineArg.get(context));
        if (mine.isEmpty()) {
            playerRef.sendMessage(Message.raw("Mine does not exist"));
            return CompletableFuture.completedFuture(null);
        }

        service.teleportToMine(playerRef, mine.get());
        return CompletableFuture.completedFuture(null);
    }
}
