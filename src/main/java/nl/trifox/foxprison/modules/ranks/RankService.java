package nl.trifox.foxprison.modules.ranks;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.util.Config;
import nl.trifox.foxprison.api.interfaces.PlayerRankService;
import nl.trifox.foxprison.framework.storage.repositories.PlayerRankRepository;
import nl.trifox.foxprison.modules.economy.EconomyManager;
import nl.trifox.foxprison.modules.ranks.config.CurrencyCostDefinition;
import nl.trifox.foxprison.modules.ranks.config.RankDefinition;
import nl.trifox.foxprison.modules.ranks.config.RanksConfig;
import nl.trifox.foxprison.modules.ranks.data.PlayerRankData;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

public final class RankService implements PlayerRankService {

    private final PlayerRankRepository store;
    private final EconomyManager economy;
    private final Config<RanksConfig> ranks;

    public RankService(PlayerRankRepository store, EconomyManager economy, Config<RanksConfig> ranks) {
        this.store = store;
        this.economy = economy;
        this.ranks = ranks;
    }

    /* =========================================================
       SYNC (no I/O): config / pure helpers
       ========================================================= */

    public RankDefinition[] getAllRanks() {
        return ranks.get().getRanks();
    }

    public Optional<RankDefinition> getRank(String rankId) {
        return ranks.get().getRank(rankId);
    }

    public int indexOfRank(String rankId) {
        RankDefinition[] all = ranks.get().getRanks();
        return indexOfRank(all, rankId);
    }

    private int indexOfRank(RankDefinition[] all, String rankId) {
        if (rankId == null) return -1;
        final String needle = rankId.trim().toLowerCase(Locale.ROOT);
        if (needle.isEmpty()) return -1;

        return IntStream.range(0, all.length)
                .filter(i -> all[i].getId() != null && all[i].getId().equalsIgnoreCase(needle))
                .findFirst()
                .orElse(-1);
    }

    /* =========================================================
       ASYNC (I/O): player rank data
       ========================================================= */

    public CompletableFuture<String> getRankIdByPlayer(UUID playerUuid) {
        return store.getOrCreate(playerUuid).thenApply(PlayerRankData::getRankId);
    }

    public CompletableFuture<Integer> getRankIndex(UUID playerUuid) {
        final RankDefinition[] all = ranks.get().getRanks();
        return getRankIdByPlayer(playerUuid).thenApply(rankId -> indexOfRank(all, rankId));
    }

    @Override
    public CompletableFuture<String> getRankID(UUID playerId) {
        return store.getOrCreate(playerId).thenApply(PlayerRankData::getRankId);
    }

    public CompletableFuture<Boolean> hasRank(UUID playerUuid, String requiredRankId) {
        final RankDefinition[] all = ranks.get().getRanks();
        final int requiredIdx = indexOfRank(all, requiredRankId);
        if (requiredIdx < 0) {
            // If the required rank doesn't exist, treat as not allowed.
            return CompletableFuture.completedFuture(false);
        }

        return getRankIdByPlayer(playerUuid)
                .thenApply(currentRankId -> indexOfRank(all, currentRankId) >= requiredIdx);
    }

    /* =========================================================
       Commands / mutations
       ========================================================= */

    public CompletableFuture<Boolean> setRankByName(CommandSender sender, PlayerRef player, String rankId) {
        UUID uuid = player.getUuid();

        var optionalRank = ranks.get().getRank(rankId);
        if (optionalRank.isEmpty()) {
            sender.sendMessage(Message.raw("Rank " + rankId + " does not exist!"));
            return CompletableFuture.completedFuture(false);
        }

        String normalized = optionalRank.get().getId();

        return store.update(uuid, data -> {
            data.setRankId(normalized);
            return data;
        }).thenApply(updated -> {
            sender.sendMessage(Message.raw("Rank set for user " + player.getUsername()));
            return true;
        });
    }

    public CompletableFuture<Boolean> rankup(PlayerRef player) {
        UUID uuid = player.getUuid();
        RankDefinition[] all = ranks.get().getRanks();

        if (!economy.isAvailable()) {
            player.sendMessage(Message.raw("Economy not available."));
            return CompletableFuture.completedFuture(false);
        }

        // Do "load -> validate -> mutate -> persist" as one update operation.
        return store.update(uuid, data -> {
            int idx = indexOfRank(all, data.getRankId());
            if (idx < 0 || idx + 1 >= all.length) {
                // keep data unchanged
                return data;
            }

            RankDefinition next = all[idx + 1];
            CurrencyCostDefinition[] costs = next.getCosts() != null
                    ? next.getCosts().getCurrencies()
                    : new CurrencyCostDefinition[0];

            // Store the "target rank" temporarily by writing it only after economy succeeds.
            // We'll just return data unchanged for now; the outer stage will handle economy and final write via save().
            // BUT since we're already in update(), we want to avoid another save.
            // So we do economy checks outside update() and only update rank if paid.
            // (Hence: this update() should NOT do economy logic.)
            return data;
        }).thenCompose(dataAfterLoad -> {
            // Now we have data loaded (and cached by repo), do the economy operations outside the repo lock.
            int idx = indexOfRank(all, dataAfterLoad.getRankId());
            if (idx < 0 || idx + 1 >= all.length) {
                player.sendMessage(Message.raw("You are max rank."));
                return CompletableFuture.completedFuture(false);
            }

            RankDefinition next = all[idx + 1];
            CurrencyCostDefinition[] costs = next.getCosts() != null
                    ? next.getCosts().getCurrencies()
                    : new CurrencyCostDefinition[0];

            for (CurrencyCostDefinition cost : costs) {
                if (!economy.hasBalance(uuid, cost.getAmount(), cost.getCurrencyId())) {
                    player.sendMessage(Message.raw(
                            "Not enough " + cost.getCurrencyId() + " to rank up. Need: " + cost.getAmount()
                    ));
                    return CompletableFuture.completedFuture(false);
                }
            }

            for (CurrencyCostDefinition cost : costs) {
                economy.withdraw(uuid, cost.getAmount(), cost.getCurrencyId());
            }

            dataAfterLoad.setRankId(next.getId());
            player.sendMessage(Message.raw("Ranked up to " + next.getDisplayName() + "!"));

            return store.save(uuid, dataAfterLoad);
        });
    }
}
