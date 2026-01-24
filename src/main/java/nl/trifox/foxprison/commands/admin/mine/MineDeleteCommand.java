package nl.trifox.foxprison.commands.admin.mine;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import nl.trifox.foxprison.service.MineService;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

public class MineDeleteCommand extends AbstractCommand {

    private final MineService service;
    private final RequiredArg<String> idArg;

    public MineDeleteCommand(MineService service) {
        super("delete", "Delete a mine");
        this.service = service;
        this.idArg = withRequiredArg("id", "Mine id", ArgTypes.STRING);
    }

    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
        String id = idArg.get(context);

        return service.deleteMine(id)
                .thenAccept(ok -> {
                    context.sender().sendMessage(Message.raw(
                            ok
                                    ? "Mine " + id + " deleted"
                                    : "Failed to delete mine (invalid id)."
                    ));
                })
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    context.sender().sendMessage(Message.raw("Failed to delete mine due to an internal error."));
                    return null;
                });
    }
}
