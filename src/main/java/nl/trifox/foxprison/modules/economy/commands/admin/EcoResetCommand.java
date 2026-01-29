package nl.trifox.foxprison.modules.economy.commands.admin;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import nl.trifox.foxprison.FoxPrisonPlugin;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.*;
import java.util.concurrent.CompletableFuture;

public class EcoResetCommand extends AbstractAsyncCommand {
    public EcoResetCommand() {
        super("reset", "Reset balance to starting amount");
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
        CommandSender sender = ctx.sender();
        if (!(sender instanceof Player player)) {
            ctx.sendMessage(Message.raw("This command can only be used by players").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        var ref = player.getReference();
        if (ref == null || !ref.isValid()) return CompletableFuture.completedFuture(null);

        var store = ref.getStore();
        var world = store.getExternalData().getWorld();

        return CompletableFuture.runAsync(() -> {
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            if (playerRef == null) return;

            double startingBalance = FoxPrisonPlugin.getInstance().getCoreConfig().get().getStartingBalance();
            FoxPrisonPlugin.getEconomyModule().getEconomyManager().setBalance(playerRef.getUuid(), startingBalance, "Admin reset");

            player.sendMessage(Message.join(
                    Message.raw("Balance reset to ").color(Color.GREEN),
                    Message.raw(String.valueOf(startingBalance)).color(new Color(50, 205, 50))
            ));
        }, world);
    }
}
