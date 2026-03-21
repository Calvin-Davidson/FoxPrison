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

    public CompletableFuture<Integer> getPrestige(UUID playerId) {
        if (cache.containsKey(playerId))
            return CompletableFuture.completedFuture(cache.get(playerId).getPrestige());

        return repository.getOrCreate(playerId).thenApply(PlayerRankData::getPrestige);
    }

    public CompletableFuture<Boolean> canPrestige(UUID playerId) {
        RankDefinition[] ranks = ranksConfig.get().getRanks();

        return getRankIndex(playerId)
                .thenApply(idx -> idx == ranks.length - 1);
    }

    public CompletableFuture<Boolean> prestige(PlayerRef player) {
        UUID uuid = player.getUuid();
        RankDefinition[] ranks = ranksConfig.get().getRanks();

        return repository.update(uuid, data -> {

            int idx = indexOfRank(ranks, data.getRankId());

            if (idx != ranks.length - 1) {
                player.sendMessage(Message.translation("foxPrison.prestige.failed.rank_to_low"));
                return data;
            }

            data.setRankId(ranks[0].getId());
            data.setPrestige(data.getPrestige() + 1);
            player.sendMessage(
                    Message.translation("foxPrison.prestige.success")
                            .param("prestige", data.getPrestige()));

            return data;

        }).thenApply(updated -> {
            cache.put(uuid, updated);
            return true;
        });
    }


    public CompletableFuture<Boolean> setRankByName(CommandSender sender, PlayerRef player, String rankId) {
        UUID uuid = player.getUuid();

        var optionalRank = ranksConfig.get().getRank(rankId);
        if (optionalRank.isEmpty()) {
            sender.sendMessage(Message.translation("foxPrison.ranks.not_exists").param("rank_id", rankId));
            return CompletableFuture.completedFuture(false);
        }

        String normalized = optionalRank.get().getId();

        return repository.update(uuid, data -> {
            data.setRankId(normalized);
            return data;
        }).thenApply(updated -> {
            cache.put(uuid, updated);

            sender.sendMessage(Message.translation("foxPrison.ranks.command.rank_set.success")
                    .param("player", player.getUsername())
                    .param("rank_id", rankId)
                    .param("rank", optionalRank.get().getDisplayName()));
            return true;
        });
    }

    /* =========================================================
       CONFIG MUTATION: used by the rank editor UI
       ========================================================= */

    public CompletableFuture<Boolean> setRankDisplayName(String rankId, String displayName) {
        var opt = ranksConfig.get().getRank(rankId);
        if (opt.isEmpty()) return CompletableFuture.completedFuture(false);
        opt.get().setDisplayName(displayName.trim());
        return ranksConfig.save().thenApply(_ -> true);
    }

    public CompletableFuture<Boolean> setRankCurrencyCost(String rankId, String currencyId, double amount) {
        var opt = ranksConfig.get().getRank(rankId);
        if (opt.isEmpty()) return CompletableFuture.completedFuture(false);
        opt.get().getCosts().setCurrencyCost(currencyId, amount);
        return ranksConfig.save().thenApply(_ -> true);
    }

    public CompletableFuture<Boolean> deleteRank(String rankId) {
        RankDefinition[] all = ranksConfig.get().getRanks();
        int idx = indexOfRank(all, rankId);
        if (idx == -1) return CompletableFuture.completedFuture(false);

        RankDefinition[] newArr = new RankDefinition[all.length - 1];
        System.arraycopy(all, 0, newArr, 0, idx);
        System.arraycopy(all, idx + 1, newArr, idx, all.length - idx - 1);
        ranksConfig.get().setRanks(newArr);
        return ranksConfig.save().thenApply(_ -> true);
    }

    public CompletableFuture<Boolean> createRank(String id, String displayName, double cost) {
        if (id == null || id.isBlank()) return CompletableFuture.completedFuture(false);
        if (ranksConfig.get().getRank(id).isPresent()) return CompletableFuture.completedFuture(false);

        RankDefinition newRank = new RankDefinition(id.trim(), displayName.trim(), cost);
        RankDefinition[] old = ranksConfig.get().getRanks();
        RankDefinition[] newArr = java.util.Arrays.copyOf(old, old.length + 1);
        newArr[newArr.length - 1] = newRank;
        ranksConfig.get().setRanks(newArr);
        return ranksConfig.save().thenApply(_ -> true);
    }

    public CompletableFuture<Boolean> rankup(PlayerRef player) {
        UUID uuid = player.getUuid();
        RankDefinition[] all = ranksConfig.get().getRanks();

        if (!economyManager.isAvailable()) {
            player.sendMessage(Message.translation("foxPrison.economy.not_available"));
            return CompletableFuture.completedFuture(false);
        }

        // Snapshot prestige from cache / DB before entering the update lock.
        // Prestige only changes via prestige(), so reading it here is safe.
        return getPrestige(uuid).thenCompose(prestigeLevel -> {

            double rankupMultiplier = 1.0 + ranksConfig.get().getPrestigeRankupMultiplier(prestigeLevel);

            return repository.update(uuid, data -> {
                int idx = indexOfRank(all, data.getRankId());
                if (idx < 0 || idx + 1 >= all.length) {
                    player.sendMessage(Message.translation("foxPrison.ranks.max_rank"));
                    return data;
                }

                RankDefinition next = all[idx + 1];
                CurrencyCostDefinition[] costs = next.getCosts() != null
                        ? next.getCosts().getCurrencies()
                        : new CurrencyCostDefinition[0];

                for (CurrencyCostDefinition cost : costs) {
                    double adjustedAmount = cost.getAmount() * rankupMultiplier;
                    if (!economyManager.hasBalance(uuid, adjustedAmount, cost.getCurrencyId())) {
                        player.sendMessage(Message.translation("foxPrison.ranks.rankup.not_enough_currency")
                                .param("currency", cost.getCurrencyId())
                                .param("amount", adjustedAmount));
                        return data; // no mutation
                    }
                }

                for (CurrencyCostDefinition cost : costs) {
                    double adjustedAmount = cost.getAmount() * rankupMultiplier;
                    economyManager.withdraw(uuid, adjustedAmount, cost.getCurrencyId());
                }

                data.setRankId(next.getId());
                player.sendMessage(Message.translation("foxPrison.ranks.rankup.success")
                        .param("rank", next.getDisplayName()));

                return data;

            }).thenApply(updated -> {
                cache.put(uuid, updated);
                return true;
            });
        });
    }
}
