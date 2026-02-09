package nl.trifox.foxprison.modules.sell.commands.player;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import nl.trifox.foxprison.modules.sell.PlayerAutoSellService;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class AutoSellCommand extends AbstractAsyncCommand {

    private final PlayerAutoSellService playerAutoSellService;

    public AutoSellCommand(PlayerAutoSellService service) {
        super("autosell", "Toggle auto sell");
        this.playerAutoSellService = service;
        requirePermission("foxprison.sell.command.autosell");
    }

    @Override
    protected @NotNull CompletableFuture<Void> executeAsync(@NotNull CommandContext context) {
        if (!(context.sender() instanceof Player player)) {
            context.sender().sendMessage(Message.raw("This command is player-only."));
            return CompletableFuture.completedFuture(null);
        }

        var newState = playerAutoSellService.toggle(player.getUuid());
        context.sender().sendMessage(Message.raw(newState ? "enabled autosell" : "disabled autosell"));

        return CompletableFuture.completedFuture(null);
    }
}