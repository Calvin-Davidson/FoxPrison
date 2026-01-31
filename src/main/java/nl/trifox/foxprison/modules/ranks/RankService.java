package nl.trifox.foxprison.modules.ranks;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.util.Config;
import nl.trifox.foxprison.framework.storage.repositories.PlayerRankRepository;
import nl.trifox.foxprison.modules.ranks.config.CurrencyCostDefinition;
import nl.trifox.foxprison.modules.ranks.data.PlayerRankData;
import nl.trifox.foxprison.modules.economy.EconomyManager;
import nl.trifox.foxprison.modules.ranks.config.RankDefinition;
import nl.trifox.foxprison.modules.ranks.config.RanksConfig;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

public class RankService
{
    private final PlayerRankRepository store;
    private final EconomyManager economy;

    private final Config<RanksConfig> ranks;

    public RankService(
            PlayerRankRepository store,
            EconomyManager economy,
            Config<RanksConfig> ranks
    ) {
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

            if (!economy.isAvailable()) {
                player.sendMessage(Message.raw("Economy not available."));
                return CompletableFuture.completedFuture(false);
            }

            RankDefinition next = all[idx + 1];
            var currencyCosts = next.getCosts().getCurrency();
            for (CurrencyCostDefinition currencyCost : currencyCosts) {
                if (!economy.hasBalance(player.getUuid(), currencyCost.getAmount(), currencyCost.getCurrencyId())) {
                    player.sendMessage(Message.raw("Not enough " + currencyCost.getCurrencyId() + " to rank up. Need: " + currencyCost.getAmount()));
                }
            }

            for (CurrencyCostDefinition currencyCost : currencyCosts) {
                economy.withdraw(player.getUuid(), currencyCost.getAmount(), currencyCost.getCurrencyId());
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
        return store.getOrCreate(player.getUuid()).thenApply(PlayerRankData::getRankId);
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
