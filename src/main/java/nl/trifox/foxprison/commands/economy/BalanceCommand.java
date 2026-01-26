package nl.trifox.foxprison.commands.economy;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import nl.trifox.foxprison.FoxPrisonPlugin;
import nl.trifox.foxprison.economy.PlayerBalance;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.concurrent.CompletableFuture;

public class BalanceCommand extends AbstractAsyncPlayerCommand {

    public BalanceCommand() {
        super("bal", "Check your balance");
        this.addAliases("balance", "money");
        this.setPermissionGroup(GameMode.Adventure);
    }

    @Override
    protected @NotNull CompletableFuture<Void> executeAsync(@NotNull CommandContext commandContext, @NotNull Store<EntityStore> store, @NotNull Ref<EntityStore> ref, @NotNull PlayerRef playerRef, @NotNull World world) {
        if (ref.isValid()) {
            return CompletableFuture.runAsync(() -> {

                FoxPrisonPlugin.getInstance().getEconomy().ensureAccount(playerRef.getUuid());
                PlayerBalance balance = FoxPrisonPlugin.getInstance().getEconomy().getPlayerBalance(playerRef.getUuid());

                if (balance == null) {
                    playerRef.sendMessage(Message.raw("Error: Could not load balance").color(Color.RED));
                    return;
                }

                String formattedBalance = balance.toString();

                playerRef.sendMessage(Message.raw("----- Your Balance -----").color(new Color(255, 215, 0)));
                playerRef.sendMessage(Message.join(
                        Message.raw("  Balance: ").color(Color.GRAY),
                        Message.raw(formattedBalance).color(new Color(50, 205, 50)).bold(true)
                ));

            });
        }
        return CompletableFuture.completedFuture(null);
    }
}