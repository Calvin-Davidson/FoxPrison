package nl.trifox.foxprison.modules.mines.commands.admin.autoreset;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import nl.trifox.foxprison.config.AutoResetDefinition;
import nl.trifox.foxprison.service.MineService;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.concurrent.CompletableFuture;

public final class MineAutoResetInfoCommand extends AbstractCommand {

    private final MineService service;
    private final RequiredArg<String> mineId;

    public MineAutoResetInfoCommand(MineService service) {
        super("info", "Show autoreset settings for a mine");
        requirePermission("nl.trifox.foxprison.admin");

        this.service = service;
        this.mineId = withRequiredArg("mineId", "Mine id", ArgTypes.STRING);
    }

    @Override
    protected CompletableFuture<Void> execute(@NonNullDecl CommandContext context) {
        String id = mineId.get(context);
        var mine = service.getMine(id);
        if (mine.isEmpty()) {
            context.sender().sendMessage(Message.raw("Mine does not exist"));
            return CompletableFuture.completedFuture(null);
        }

        AutoResetDefinition ar = mine.get().getAutoReset();
        if (ar == null) {
            context.sender().sendMessage(Message.raw("Mine not found: " + id));
            return CompletableFuture.completedFuture(null);
        }

        context.sender().sendMessage(Message.raw(
                "AutoReset for '" + id + "': " +
                        "enabled=" + ar.isEnabled() +
                        ", intervalSeconds=" + ar.getIntervalSeconds() +
                        ", blocksBrokenThreshold=" + ar.getBlocksBrokenThreshold() +
                        ", minSecondsBetweenResets=" + ar.getMinSecondsBetweenResets()
        ));
        return CompletableFuture.completedFuture(null);
    }
}
