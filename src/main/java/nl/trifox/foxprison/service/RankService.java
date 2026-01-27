package nl.trifox.foxprison.service;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.util.Config;
import nl.trifox.foxprison.FoxPrisonPlugin;
import nl.trifox.foxprison.config.*;
import nl.trifox.foxprison.config.mines.MinesConfig;
import nl.trifox.foxprison.data.player.PlayerDataStore;
import nl.trifox.foxprison.data.player.PlayerPrisonData;
import nl.trifox.foxprison.economy.EconomyManager;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

public class RankService
{
    private final FoxPrisonPlugin plugin;
    private final PlayerDataStore store;
    private final EconomyManager economy;

    private final Config<RanksConfig> ranks;

    public RankService(
            FoxPrisonPlugin plugin,
            PlayerDataStore store,
            EconomyManager economy,
            Config<CoreConfig> core,
            Config<RanksConfig> ranks,
            Config<MinesConfig> mines
    ) {
        this.plugin = plugin;
        this.store = store;
        this.economy = economy;
        this.ranks = ranks;
    }

    public CompletableFuture<Boolean> rankup(PlayerRef player) {
        UUID uuid = player.getUuid();

        return store.getOrCreate(uuid).thenCompose(data -> {
            RankDefinition[] all = ranks.get().getRanks();

            int idx = indexOfRank(all, data.getRankId());
            if (idx < 0 || idx + 1 >= all.length) {
                player.sendMessage(Message.raw("You are max rank."));
                return CompletableFuture.completedFuture(false);
            }

            RankDefinition next = all[idx + 1];
            double cost = next.getCost();

            if (!economy.isAvailable()) {
                player.sendMessage(Message.raw("Economy not available."));
                return CompletableFuture.completedFuture(false);
            }

            if (!economy.withdraw(uuid, cost, "rankup")) {
                player.sendMessage(Message.raw("Not enough money to rank up. Need: " + cost));
                return CompletableFuture.completedFuture(false);
            }

            data.setRankId(next.getId());
            player.sendMessage(Message.raw("Ranked up to " + next.getDisplayName() + "!"));

            return store.save(uuid, data);
        });
    }

    public CompletableFuture<Boolean> setRankByName(CommandSender sender, PlayerRef player, String rankId) {
        UUID uuid = player.getUuid();

        return store.getOrCreate(uuid).thenCompose(data -> {
            var optionalRank = ranks.get().getRank(rankId);
            if (optionalRank.isEmpty()) {
                sender.sendMessage(Message.raw("Ranked " + rankId + " does not exist!"));
                return CompletableFuture.completedFuture(false);
            }

            data.setRankId(optionalRank.get().getId());
            sender.sendMessage(Message.raw("rank set for user " + player.getUsername()));
            return store.save(uuid, data);
        });
    }

    public CompletableFuture<String> getRankIdByPlayer(PlayerRef player) {
        return store.getOrCreate(player.getUuid()).thenApply(PlayerPrisonData::getRankId);
    }

    private int indexOfRank(RankDefinition[] ranks, String rankId) {
        return IntStream.range(0, ranks.length).filter(i -> ranks[i].getId().equalsIgnoreCase(rankId)).findFirst().orElse(-1);
    }

    public RankDefinition[] getAllRanks() {
        return ranks.get().getRanks();
    }

    public Optional<RankDefinition> getRank(String rankID) {
        return ranks.get().getRank(rankID);
    }
}
