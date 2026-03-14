// UPDATED - uses EconomyManager interface, no FoxEconomyManager cast
package nl.trifox.foxprison.modules.economy.command.player;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import nl.trifox.foxprison.FoxPrisonPlugin;
import nl.trifox.foxprison.modules.economy.data.LeaderboardEntry;
import nl.trifox.foxprison.modules.economy.data.PlayerBalanceData;
import nl.trifox.foxprison.modules.economy.manager.FoxEconomyManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class BalTopCommand extends AbstractAsyncPlayerCommand {

    private static final int DEFAULT_PAGE_SIZE = 10;

    public BalTopCommand() {
        super("baltop", "Get the top ranking economy players");
        addAliases("balancetop", "moneytop");
        this.requirePermission("foxprison.eco.command.baltop");
    }

    @Override
    protected @NotNull CompletableFuture<Void> executeAsync(
            @NotNull CommandContext commandContext,
            @NotNull Store<EntityStore> store,
            @NotNull Ref<EntityStore> ref,
            @NotNull PlayerRef playerRef,
            @NotNull World world
    ) {
        return CompletableFuture.runAsync(() -> {
            var economyManager = FoxPrisonPlugin.getEconomyModule().getEconomyManager();

            String currency = economyManager.getDefaultCurrencyID();
            List<LeaderboardEntry> leaderboard = economyManager.getLeaderboard(DEFAULT_PAGE_SIZE, currency);

            if (leaderboard.isEmpty()) {
                playerRef.sendMessage(Message.translation("foxPrison.economy.no_data_available"));
                return;
            }

            playerRef.sendMessage(Message.translation("foxPrison.economy.balancetop.header")
                    .param("amount", leaderboard.size()));

            for (int i = 0; i < leaderboard.size(); i++) {
                var entry = leaderboard.get(i);
                double balance = entry.balance();

                String name = resolvePlayerName(entry.playerUuid());
                String formattedBalance = economyManager.format(balance, currency);

                playerRef.sendMessage(Message.translation("foxPrison.economy.balancetop.entry")
                        .param("rank", "#" + (i+1))
                        .param("player", name)
                        .param("balance", formattedBalance)
                        .param("currency",  economyManager.getCurrencySingular(currency)));
            }
        });
    }

    private String resolvePlayerName(UUID uuid) {
        var player = Universe.get().getPlayer(uuid);
        if (player != null) return player.getUsername();
        // Offline player fallback - show shortened UUID until name lookup is available
        return uuid.toString().substring(0, 8) + "...";
    }
}