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
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

public final class RankService implements PlayerRankService {

    private final PlayerRankRepository repository;
    private final EconomyManager economyManager;
    private final Config<RanksConfig> ranksConfig;

    private ConcurrentHashMap<UUID, PlayerRankData> cache;

    public RankService(PlayerRankRepository store, EconomyManager economy, Config<RanksConfig> ranks) {
        this.repository = store;
        this.economyManager = economy;
        this.ranksConfig = ranks;
        this.cache = new ConcurrentHashMap<>();
    }

    public CompletableFuture<Boolean> setupPlayer(UUID playerUuid) {
        return repository.getOrCreate(playerUuid).thenApply(playerRankData -> {
            cache.put(playerUuid, playerRankData);
            return true;
        });
    }

    /* =========================================================
       SYNC (no I/O): config / pure helpers
       ========================================================= */

    public RankDefinition[] getAllRanks() {
        return ranksConfig.get().getRanks();
    }

    public Optional<RankDefinition> getRank(String rankId) {
        return ranksConfig.get().getRank(rankId);
    }

    public int indexOfRank(String rankId) {
        RankDefinition[] all = ranksConfig.get().getRanks();
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


    public CompletableFuture<Integer> getRankIndex(UUID playerUuid) {
        final RankDefinition[] all = ranksConfig.get().getRanks();
        return getRankID(playerUuid).thenApply(rankId -> indexOfRank(all, rankId));
    }

    @Override
    public CompletableFuture<String> getRankID(UUID playerId) {
        if (cache.containsKey(playerId)) return CompletableFuture.completedFuture(cache.get(playerId).getRankId());
        return repository.getOrCreate(playerId).thenApply(PlayerRankData::getRankId);
    }

    public CompletableFuture<Boolean> hasRank(UUID playerUuid, String requiredRankId) {
        final RankDefinition[] all = ranksConfig.get().getRanks();
        final int requiredIdx = indexOfRank(all, requiredRankId);
        if (requiredIdx < 0) {
            // If the required rank doesn't exist, treat as not allowed.
            return CompletableFuture.completedFuture(false);
        }

        return getRankID(playerUuid)
                .thenApply(currentRankId -> indexOfRank(all, currentRankId) >= requiredIdx);
    }



    public CompletableFuture<Boolean> setRankByName(CommandSender sender, PlayerRef player, String rankId) {
        UUID uuid = player.getUuid();

        var optionalRank = ranksConfig.get().getRank(rankId);
        if (optionalRank.isEmpty()) {
            sender.sendMessage(Message.translation("ranks.not_exist").param("rank", rankId));
            return CompletableFuture.completedFuture(false);
        }

        String normalized = optionalRank.get().getId();

        return repository.update(uuid, data -> {
            data.setRankId(normalized);
            return data;
        }).thenApply(updated -> {
            sender.sendMessage(Message.translation("foxPrison.ranks.rank_set.success")
                    .param("player", player.getUsername())
                    .param("rank", rankId));
            return true;
        });
    }

    public CompletableFuture<Boolean> rankup(PlayerRef player) {
        UUID uuid = player.getUuid();
        RankDefinition[] all = ranksConfig.get().getRanks();

        if (!economyManager.isAvailable()) {
            player.sendMessage(Message.raw("Economy not available."));
            return CompletableFuture.completedFuture(false);
        }

        // Do "load -> validate -> mutate -> persist" as one update operation.
        return repository.update(uuid, data -> {
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
                if (!economyManager.hasBalance(uuid, cost.getAmount(), cost.getCurrencyId())) {
                    player.sendMessage(Message.raw(
                            "Not enough " + cost.getCurrencyId() + " to rank up. Need: " + cost.getAmount()
                    ));
                    return CompletableFuture.completedFuture(false);
                }
            }

            for (CurrencyCostDefinition cost : costs) {
                economyManager.withdraw(uuid, cost.getAmount(), cost.getCurrencyId());
            }

            dataAfterLoad.setRankId(next.getId());
            player.sendMessage(Message.raw("Ranked up to " + next.getDisplayName() + "!"));

            return repository.save(uuid, dataAfterLoad);
        });
    }
}
