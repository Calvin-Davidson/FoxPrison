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

public class EcoGiveCommand extends AbstractAsyncCommand {
    private final RequiredArg<Double> amountArg;

    public EcoGiveCommand() {
        super("give", "Add money to your balance");
        this.addAliases("add");
        this.amountArg = this.withRequiredArg("amount", "The amount to add", ArgTypes.DOUBLE);
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
        CommandSender sender = ctx.sender();
        if (!(sender instanceof Player player)) {
            ctx.sendMessage(Message.raw("This command can only be used by players").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        Double amount = amountArg.get(ctx);
        if (amount == null || amount <= 0) {
            ctx.sendMessage(Message.raw("Amount must be positive").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        var ref = player.getReference();
        if (ref == null || !ref.isValid()) {
            return CompletableFuture.completedFuture(null);
        }

        var store = ref.getStore();
        var world = store.getExternalData().getWorld();

        // Run on world thread to avoid threading issues
        return CompletableFuture.runAsync(() -> {
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            if (playerRef == null) return;

            FoxPrisonPlugin.getEconomyModule().getEconomyManager().deposit(playerRef.getUuid(), amount, "Admin give");
            double newBalance = FoxPrisonPlugin.getEconomyModule().getEconomyManager().getBalance(playerRef.getUuid());

            player.sendMessage(Message.join(
                    Message.raw("Added ").color(Color.GREEN),
                    Message.raw("+" + FoxPrisonPlugin.getInstance().getCoreConfig().get().format(amount)).color(new Color(50, 205, 50)),
                    Message.raw(" | New balance: ").color(Color.GRAY),
                    Message.raw(FoxPrisonPlugin.getInstance().getCoreConfig().get().format(newBalance)).color(Color.WHITE)
            ));
        }, world);
    }
}
