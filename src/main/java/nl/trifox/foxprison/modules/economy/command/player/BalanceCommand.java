package nl.trifox.foxprison.modules.economy.command.player;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import nl.trifox.foxprison.FoxPrisonPlugin;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.concurrent.CompletableFuture;

public class BalanceCommand extends AbstractAsyncPlayerCommand {

    public BalanceCommand() {
        super("bal", "Check your balance");
        this.addAliases("balance", "money");
        this.setPermissionGroup(GameMode.Adventure);

        this.requirePermission("foxprison.eco.command.balance");
    }

    @Override
    protected @NotNull CompletableFuture<Void> executeAsync(@NotNull CommandContext commandContext, @NotNull Store<EntityStore> store, @NotNull Ref<EntityStore> ref, @NotNull PlayerRef playerRef, @NotNull World world) {
        if (ref.isValid()) {
            return CompletableFuture.runAsync(() -> {

                FoxPrisonPlugin.getEconomyModule().getEconomyManager().ensureAccount(playerRef.getUuid());
                var balance = FoxPrisonPlugin.getEconomyModule().getEconomyManager().getBalance(playerRef.getUuid());

                playerRef.sendMessage(Message.raw("----- Your Balance -----").color(new Color(255, 215, 0)));
                playerRef.sendMessage(Message.join(
                        Message.raw("  Balance: ").color(Color.GRAY),
                        Message.raw(FoxPrisonPlugin.getInstance().getEconomyConfig().get().getDefaultCurrency().format(balance)).color(new Color(50, 205, 50)).bold(true)
                ));

            });
        }
        return CompletableFuture.completedFuture(null);
    }
}