package nl.trifox.foxprison.modules.economy.command.admin;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import nl.trifox.foxprison.FoxPrisonPlugin;
import nl.trifox.foxprison.modules.economy.config.CurrencyDefinition;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.concurrent.CompletableFuture;

public  class EcoTakeCommand extends AbstractAsyncEconomyAdminCommand {
    private final RequiredArg<Double> amountArg;

    public EcoTakeCommand() {
        super("take", "Remove money from your balance");
        this.addAliases("remove");
        this.amountArg = this.withRequiredArg("amount", "The amount to remove", ArgTypes.DOUBLE);

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

        return CompletableFuture.runAsync(() -> {
            PlayerRef playerRef = store.getComponent(targetRef, PlayerRef.getComponentType());
            if (playerRef == null) return;

            boolean success = FoxPrisonPlugin.getEconomyModule().getEconomyManager().withdraw(playerRef.getUuid(), amount, "Admin take", currency.getId());
            double newBalance = FoxPrisonPlugin.getEconomyModule().getEconomyManager().getBalance(playerRef.getUuid(), currency.getId());

            if (success) {
                context.sender().sendMessage(Message.join(
                        Message.raw("Removed ").color(Color.YELLOW),
                        Message.raw("-" + FoxPrisonPlugin.getInstance().getEconomyConfig().get().getCurrency(currency.getId()).format(amount)).color(new Color(255, 99, 71)),
                        Message.raw(" | New balance: ").color(Color.GRAY),
                        Message.raw(FoxPrisonPlugin.getInstance().getEconomyConfig().get().getCurrency(currency.getId()).format(newBalance)).color(Color.WHITE)
                ));
            } else {
                context.sender().sendMessage(Message.raw("Insufficient funds").color(Color.RED));
            }
        }, world);
    }
}
