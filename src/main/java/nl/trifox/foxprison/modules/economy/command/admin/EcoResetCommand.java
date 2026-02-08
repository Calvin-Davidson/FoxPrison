package nl.trifox.foxprison.modules.economy.command.admin;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import nl.trifox.foxprison.FoxPrisonPlugin;
import nl.trifox.foxprison.modules.economy.config.CurrencyDefinition;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.concurrent.CompletableFuture;

public class EcoResetCommand extends AbstractAsyncEconomyAdminCommand {
    public EcoResetCommand() {
        super("reset", "Reset balance to starting amount");
        requirePermission("foxprison.eco.command.reset");
    }


    @Override
    protected CompletableFuture<Void> executeAsync(@NotNull CommandContext context, Ref<EntityStore> targetRef, CurrencyDefinition currency) {

        var store = targetRef.getStore();
        var world = store.getExternalData().getWorld();

        return CompletableFuture.runAsync(() -> {
            PlayerRef playerRef = store.getComponent(targetRef, PlayerRef.getComponentType());
            if (playerRef == null) return;

            double startingBalance = FoxPrisonPlugin.getInstance().getEconomyConfig().get().getCurrency(currency.getId()).getStartingBalance();
            getEconomyManager().setBalance(playerRef.getUuid(), startingBalance, "Admin reset");

            context.sender().sendMessage(Message.join(
                    Message.raw("Balance reset to ").color(Color.GREEN),
                    Message.raw(String.valueOf(startingBalance)).color(new Color(50, 205, 50))
            ));
        }, world);
    }
}
