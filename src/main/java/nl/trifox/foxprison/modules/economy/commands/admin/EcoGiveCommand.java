package nl.trifox.foxprison.modules.economy.commands.admin;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import nl.trifox.foxprison.FoxPrisonPlugin;
import nl.trifox.foxprison.modules.economy.config.CurrencyDefinition;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.concurrent.CompletableFuture;

public class EcoGiveCommand extends AbstractAsyncEconomyAdminCommand {
    private final RequiredArg<Double> amountArg;

    public EcoGiveCommand() {
        super("give", "Add money to your balance");
        this.addAliases("add");
        this.amountArg = this.withRequiredArg("amount", "The amount to add", ArgTypes.DOUBLE);
    }

    @Override
    protected CompletableFuture<Void> executeAsync(@NotNull CommandContext context, Ref<EntityStore> targetRef, CurrencyDefinition currency) {
        Double amount = amountArg.get(context);
        if (amount == null || amount <= 0) {
            context.sendMessage(Message.raw("Amount must be positive").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        var store = targetRef.getStore();
        var world = store.getExternalData().getWorld();

        PlayerRef playerRef = store.getComponent(targetRef, PlayerRef.getComponentType());
        if (playerRef == null) return CompletableFuture.completedFuture(null);

        return CompletableFuture.runAsync(() -> {
            FoxPrisonPlugin.getEconomyModule().getEconomyManager().deposit(playerRef.getUuid(), amount, "Admin give");
            double newBalance = FoxPrisonPlugin.getEconomyModule().getEconomyManager().getBalance(playerRef.getUuid());

            context.sender().sendMessage(Message.join(
                    Message.raw("Added ").color(Color.GREEN),
                    Message.raw("+" + FoxPrisonPlugin.getInstance().getEconomyConfig().get().getDefaultCurrency().format(amount)).color(new Color(50, 205, 50)),
                    Message.raw(" | New balance: ").color(Color.GRAY),
                    Message.raw(FoxPrisonPlugin.getInstance().getEconomyConfig().get().getDefaultCurrency().format(newBalance)).color(Color.WHITE)
            ));
        }, world);
    }
}
