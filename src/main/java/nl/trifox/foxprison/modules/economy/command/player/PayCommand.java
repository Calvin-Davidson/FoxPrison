package nl.trifox.foxprison.modules.economy.command.player;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import nl.trifox.foxprison.FoxPrisonPlugin;
import nl.trifox.foxprison.modules.economy.enums.TransferResult;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.concurrent.CompletableFuture;

public class PayCommand extends AbstractAsyncPlayerCommand {
    private final RequiredArg<Double> amountArg;
    private final RequiredArg<PlayerRef> targetPlayerArg;

    public PayCommand() {
        super("pay", "pay balance to a playrer");
        this.amountArg = this.withRequiredArg("amount", "The amount to remove", ArgTypes.DOUBLE);
        this.targetPlayerArg = this.withRequiredArg("target player", "The player whom will receive this balance", ArgTypes.PLAYER_REF);
    }

    @Override
    protected @NotNull CompletableFuture<Void> executeAsync(@NotNull CommandContext commandContext, @NotNull Store<EntityStore> store, @NotNull Ref<EntityStore> ref, @NotNull PlayerRef playerRef, @NotNull World world) {
        Double amount = amountArg.get(commandContext);
        if (amount == null || amount <= 0) {
            commandContext.sendMessage(Message.raw("Amount must be positive").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        var targetPlayer = targetPlayerArg.get(commandContext);
        if (!targetPlayer.isValid()){
            commandContext.sendMessage(Message.raw("Invalid target player").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }
        var transferResult = FoxPrisonPlugin.getEconomyModule().getEconomyManager().transfer(playerRef.getUuid(), targetPlayer.getUuid(), amount, "pay transfer");

        if (transferResult == TransferResult.SELF_TRANSFER) {
            commandContext.sendMessage(Message.raw("Cannot transfer money to yourself").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }
        double newBalance = FoxPrisonPlugin.getEconomyModule().getEconomyManager().getBalance(playerRef.getUuid());

        if (transferResult == TransferResult.SUCCESS) {
            playerRef.sendMessage(Message.join(
                    Message.raw("Transfered ").color(Color.YELLOW),
                    Message.raw("-" + FoxPrisonPlugin.getInstance().getEconomyConfig().get().getDefaultCurrency().format(amount)).color(new Color(255, 99, 71)),
                    Message.raw(" | New balance: ").color(Color.GRAY),
                    Message.raw(FoxPrisonPlugin.getInstance().getEconomyConfig().get().getDefaultCurrency().format(newBalance)).color(Color.WHITE)
            ));

            targetPlayer.sendMessage(Message.join(
                    Message.raw("Received ").color(Color.YELLOW),
                    Message.raw("-" + FoxPrisonPlugin.getInstance().getEconomyConfig().get().getDefaultCurrency().format(amount)).color(new Color(255, 99, 71)),
                    Message.raw(" | New balance: ").color(Color.GRAY),
                    Message.raw(FoxPrisonPlugin.getInstance().getEconomyConfig().get().getDefaultCurrency().format(newBalance)).color(Color.WHITE)
            ));
        } else {
            playerRef.sendMessage(Message.raw("Insufficient funds").color(Color.RED));
        }
        return CompletableFuture.completedFuture(null);
    }
}
