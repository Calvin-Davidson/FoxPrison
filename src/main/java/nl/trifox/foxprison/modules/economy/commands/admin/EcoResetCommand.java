package nl.trifox.foxprison.modules.economy.commands.admin;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
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

public class EcoResetCommand extends AbstractAsyncEconomyAdminCommand {
    public EcoResetCommand() {
        super("reset", "Reset balance to starting amount");
    }


    @Override
    protected CompletableFuture<Void> executeAsync(@NotNull CommandContext context, Ref<EntityStore> targetRef, CurrencyDefinition currency) {

        var store = targetRef.getStore();
        var world = store.getExternalData().getWorld();

        PlayerRef playerRef = store.getComponent(targetRef, PlayerRef.getComponentType());

        if (playerRef == null) return CompletableFuture.completedFuture(null);

        return CompletableFuture.runAsync(() -> {
            double startingBalance = FoxPrisonPlugin.getInstance().getEconomyConfig().get().getDefaultCurrency().getStartingBalance();
            FoxPrisonPlugin.getEconomyModule().getEconomyManager().setBalance(playerRef.getUuid(), startingBalance, "Admin reset");

            context.sender().sendMessage(Message.join(
                    Message.raw("Balance reset to ").color(Color.GREEN),
                    Message.raw(String.valueOf(startingBalance)).color(new Color(50, 205, 50))
            ));
        }, world);
    }
}
