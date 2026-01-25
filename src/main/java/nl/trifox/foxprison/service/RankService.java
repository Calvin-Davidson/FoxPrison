package nl.trifox.foxprison.service;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.util.Config;
import nl.trifox.foxprison.FoxPrisonPlugin;
import nl.trifox.foxprison.config.*;
import nl.trifox.foxprison.data.PlayerDataStore;
import nl.trifox.foxprison.data.PlayerPrisonData;
import nl.trifox.foxprison.economy.Economy;

import java.math.BigDecimal;
import java.util.List;

public class RankService
{
    private final FoxPrisonPlugin plugin;
    private final PlayerDataStore store;
    private final Economy economy;

    private final Config<EconomyConfig> econCfg;
    private final Config<RanksConfig> ranks;

    public RankService(
            FoxPrisonPlugin plugin,
            PlayerDataStore store,
            Economy economy,
            Config<CoreConfig> core,
            Config<EconomyConfig> econCfg,
            Config<RanksConfig> ranks,
            Config<MinesConfig> mines
    ) {
        this.plugin = plugin;
        this.store = store;
        this.economy = economy;
        this.econCfg = econCfg;
        this.ranks = ranks;
    }
    public void rankup(PlayerRef player) {

        PlayerPrisonData data = store.getOrCreate(player.getUuid());
        List<RankDefinition> all = ranks.get().getRanks();

        int idx = indexOfRank(all, data.getRankId());
        if (idx < 0 || idx + 1 >= all.size()) {
            player.sendMessage(Message.raw("You are max rank."));
            return;
        }

        RankDefinition next = all.get(idx + 1);
        double cost = next.getCost();

        if (!economy.isAvailable()) {
            player.sendMessage(Message.raw("Economy not available."));
            return;
        }

        if (!economy.withdraw(player.getUuid(), BigDecimal.valueOf(cost))) {
            player.sendMessage(Message.raw("Not enough money to rank up. Need: " + cost));
            return;
        }

        data.setRankId(next.getId());
        player.sendMessage(Message.raw("Ranked up to " + next.getDisplayName() + "!"));
    }
    public void sellAll(PlayerRef player) {
        if (!economy.isAvailable()) {
            player.sendMessage(Message.raw("Economy not available."));
            return;
        }

        double multiplier = econCfg.get().getSellMultiplier();

        // TODO: Scan inventory, compute sell price, clear items, then deposit.
        double examplePayout = 0.0;

        if (examplePayout <= 0) {
            player.sendMessage(Message.raw("Nothing to sell."));
            return;
        }

        economy.deposit(player.getUuid(), BigDecimal.valueOf(examplePayout * multiplier));
        player.sendMessage(Message.raw("Sold all for " + (examplePayout * multiplier)));
    }
    public void setRankByName(String playerName, String rankId) {
        // TODO: resolve player by name (Universe), then set data
        plugin.getLogger().atInfo().log("TODO: setRank " + playerName + " -> " + rankId);
    }
    private int indexOfRank(List<RankDefinition> ranks, String rankId) {
        for (int i = 0; i < ranks.size(); i++) {
            if (ranks.get(i).getId().equalsIgnoreCase(rankId)) return i;
        }
        return -1;
    }

}
