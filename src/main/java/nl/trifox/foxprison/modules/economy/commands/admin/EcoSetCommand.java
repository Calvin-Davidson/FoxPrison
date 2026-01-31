package nl.trifox.foxprison.modules.economy.commands.admin;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import nl.trifox.foxprison.FoxPrisonPlugin;
import nl.trifox.foxprison.modules.economy.EconomyManager;
import nl.trifox.foxprison.modules.economy.config.CurrencyDefinition;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.concurrent.CompletableFuture;

public class EcoSetCommand extends AbstractAsyncEconomyAdminCommand {
    private final RequiredArg<Double> amountArg;

    public EcoSetCommand() {
        super("set", "Set your balance to a specific amount");
        this.amountArg = this.withRequiredArg("amount", "The amount to set", ArgTypes.DOUBLE);
    }

    @Override
    protected CompletableFuture<Void> executeAsync(@NotNull CommandContext context, Ref<EntityStore> targetRef, CurrencyDefinition currency) {
        Double amount = amountArg.get(context);
        if (amount == null || amount < 0) {
            context.sendMessage(Message.raw("Amount must be positive").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        var store = targetRef.getStore();
        var world = store.getExternalData().getWorld();

        PlayerRef playerRef = store.getComponent(targetRef, PlayerRef.getComponentType());
        if (playerRef == null) return CompletableFuture.completedFuture(null);

        return CompletableFuture.runAsync(() -> {
            double oldBalance = getEconomyManager().getBalance(playerRef.getUuid(), currency.getId());
            getEconomyManager().setBalance(playerRef.getUuid(), amount,"Admin set", currency.getId());

            context.sender().sendMessage(Message.join(
                    Message.raw("Balance set: ").color(Color.GREEN),
                    Message.raw(currency.format(oldBalance)).color(Color.GRAY),
                    Message.raw(" -> ").color(Color.WHITE),
                    Message.raw(currency.format(amount)).color(new Color(50, 205, 50))
            ));
        }, world);
    }
}