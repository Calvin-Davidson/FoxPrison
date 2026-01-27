package nl.trifox.foxprison.modules.mines.commands.admin;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncPlayerCommand;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.prefab.selection.mask.BlockPattern;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import nl.trifox.foxprison.service.MineService;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.concurrent.CompletableFuture;

public class MineSetBlockPatternCommand extends AbstractAsyncPlayerCommand {

    private final MineService service;

    private final RequiredArg<String> mineId;
    private final RequiredArg<BlockPattern> blockPattern;

    public MineSetBlockPatternCommand(MineService service) {
        super("setpattern", "Change the blockpattern of a mine");
        this.service = service;

        mineId = withRequiredArg("mineId", "Mine id", ArgTypes.STRING);
        blockPattern = withRequiredArg("blockTypeId", "Block type id (e.g. hytale:stone)", ArgTypes.BLOCK_PATTERN);
    }


    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(@NonNullDecl CommandContext commandContext, @NonNullDecl Store<EntityStore> store, @NonNullDecl Ref<EntityStore> ref, @NonNullDecl PlayerRef playerRef, @NonNullDecl World world) {

        return service.SetSpawnableBlockPattern(mineId.get(commandContext), blockPattern.get(commandContext)).thenAccept(ok -> {
                    commandContext.sender().sendMessage(Message.raw(
                            ok
                                    ? "changed the block pattern"
                                    : "Failed to add region to mine (invalid id)."
                    ));
                })
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    commandContext.sender().sendMessage(Message.raw("Failed to create mine due to an internal error."));
                    return null;
                });
    }
}
