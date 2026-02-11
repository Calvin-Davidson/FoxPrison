package nl.trifox.foxprison.modules.mines.commands.admin.autoreset;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import nl.trifox.foxprison.modules.mines.MineService;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.concurrent.CompletableFuture;

public final class MineAutoResetSetCommand extends AbstractCommand {

    private final MineService service;

    private final RequiredArg<String> mineId;
    private final RequiredArg<Boolean> enabled;
    private final RequiredArg<Integer> intervalSeconds;
    private final RequiredArg<Integer> blocksBrokenThreshold;
    private final OptionalArg<Integer> minSecondsBetweenResets;

    public MineAutoResetSetCommand(MineService service) {
        super("set", "Set mine auto reset settings");
        requirePermission("foxprison.mine.command.admin.autoreset.set");

        this.service = service;

        this.mineId = withRequiredArg("mineId", "Mine id", ArgTypes.STRING);
        this.enabled = withRequiredArg("enabled", "true/false", ArgTypes.BOOLEAN);
        this.intervalSeconds = withRequiredArg("intervalSeconds", "0 disables interval reset", ArgTypes.INTEGER);
        this.blocksBrokenThreshold = withRequiredArg("blocksBrokenThreshold", "0 disables block-break reset", ArgTypes.INTEGER);
        this.minSecondsBetweenResets = withOptionalArg("minSecondsBetweenResets", "Cooldown seconds (optional)", ArgTypes.INTEGER);
    }

    @Override
    protected CompletableFuture<Void> execute(@NonNullDecl CommandContext context) {
        String id = mineId.get(context);
        var optionalMine = service.getMine(id);
        if (optionalMine.isEmpty()) {
            context.sender().sendMessage(Message.translation("foxPrison.mines.not_exist"));
            return CompletableFuture.completedFuture(null);
        }

        var existingMineResetDelay = optionalMine.get().getAutoReset().getMinSecondsBetweenResets();

        return service.setAutoReset(
                id,
                enabled.get(context),
                intervalSeconds.get(context),
                blocksBrokenThreshold.get(context),
                minSecondsBetweenResets.provided(context) ? minSecondsBetweenResets.get(context) : existingMineResetDelay
        ).thenApply(_ -> {
            context.sender().sendMessage(Message.translation("foxPrison.mines.commands.autoreset.set.success"));
            return null;
        });
    }
}
