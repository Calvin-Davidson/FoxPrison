package nl.trifox.foxprison.modules.mines.commands.admin;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import nl.trifox.foxprison.modules.mines.MineService;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.Random;
import java.util.concurrent.CompletableFuture;

public class MineResetCommand extends AbstractAsyncCommand {
    private final RequiredArg<String> mineId;
    private final MineService service;

    public MineResetCommand(MineService service) {
        super("reset", "resets the mine by filling the block");
        this.service = service;

        mineId = withRequiredArg("mineId", "Mine id", ArgTypes.STRING);

    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(@NonNullDecl CommandContext context) {
        var mineOpt = service.getAllMines().stream()
                .filter(x -> x.getId().equalsIgnoreCase(mineId.get(context)))
                .findFirst();

        if (mineOpt.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        service.resetMine(mineId.get(context));

        context.sender().sendMessage(Message.raw("Mine resetted"));
        return CompletableFuture.completedFuture(null);
    }
}
