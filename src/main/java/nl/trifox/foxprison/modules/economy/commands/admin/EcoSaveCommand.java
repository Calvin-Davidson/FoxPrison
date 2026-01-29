package nl.trifox.foxprison.modules.economy.commands.admin;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import nl.trifox.foxprison.FoxPrisonPlugin;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.*;
import java.util.concurrent.CompletableFuture;

public class EcoSaveCommand extends AbstractAsyncCommand {
    public EcoSaveCommand() {
        super("save", "Force save all data");
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
        FoxPrisonPlugin.getEconomyModule().getEconomyManager().forceSave();
        ctx.sendMessage(Message.raw("✓ Economy data saved successfully").color(Color.GREEN));
        return CompletableFuture.completedFuture(null);
    }
}
