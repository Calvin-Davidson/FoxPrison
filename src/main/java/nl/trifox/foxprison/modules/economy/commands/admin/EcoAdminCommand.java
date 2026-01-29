package nl.trifox.foxprison.modules.economy.commands.admin;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import nl.trifox.foxprison.FoxPrisonPlugin;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.*;
import java.util.concurrent.CompletableFuture;

public class EcoAdminCommand extends AbstractAsyncCommand {

    public EcoAdminCommand() {
        super("eco", "Economy administration commands");
        this.addAliases("economy", "ecoadmin");
        this.setPermissionGroup(null); // Admin only - requires ecotale.ecotale.command.eco permission

        this.addSubCommand(new EcoSetCommand());
        this.addSubCommand(new EcoGiveCommand());
        this.addSubCommand(new EcoTakeCommand());
        this.addSubCommand(new EcoResetCommand());
        this.addSubCommand(new EcoSaveCommand());
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext commandContext) {
        CommandSender sender = commandContext.sender();

        // Fallback: show help text
        commandContext.sender().sendMessage(Message.raw("=== Ecotale Economy Admin ===").color(new Color(255, 215, 0)));
        commandContext.sender().sendMessage(Message.raw("  /eco set <amount> - Set your balance").color(Color.GRAY));
        commandContext.sender().sendMessage(Message.raw("  /eco give <amount> - Add to balance").color(Color.GRAY));
        commandContext.sender().sendMessage(Message.raw("  /eco take <amount> - Remove from balance").color(Color.GRAY));
        commandContext.sender().sendMessage(Message.raw("  /eco reset - Reset to starting balance").color(Color.GRAY));
        commandContext.sender().sendMessage(Message.raw("  /eco save - Force save data").color(Color.GRAY));
        return CompletableFuture.completedFuture(null);
    }
}