package nl.trifox.foxprison.modules.mines.commands.admin.region;

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
import nl.trifox.foxprison.service.MineService;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.concurrent.CompletableFuture;

public class RegionClearCommand extends AbstractAsyncPlayerCommand {

    private final MineService service;
    private final RequiredArg<String> mineId;

    public RegionClearCommand(MineService service) {
        super("clear", "Clear all boxes from a mine region");
        this.service = service;

        mineId = withRequiredArg("mineId", "Mine id", ArgTypes.STRING);
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(@NonNullDecl CommandContext commandContext, @NonNullDecl Store<EntityStore> store, @NonNullDecl Ref<EntityStore> ref, @NonNullDecl PlayerRef playerRef, @NonNullDecl World world) {
        var mineID = mineId.get(commandContext);
        return service.regionClear(mineID)
                .thenAccept(ok -> {
                    commandContext.sender().sendMessage(Message.raw(
                            ok
                                    ? "region cleared for mine: " + mineID
                                    : "Failed to create mine (duplicate id or invalid id)."
                    ));
                })
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    commandContext.sender().sendMessage(Message.raw("Failed to create mine due to an internal error."));
                    return null;
                });
    }
}
