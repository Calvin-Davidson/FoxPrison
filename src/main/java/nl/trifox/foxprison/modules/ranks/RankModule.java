package nl.trifox.foxprison.modules.ranks;

import nl.trifox.foxprison.FoxPrisonPlugin;
import nl.trifox.foxprison.framework.module.FoxModule;
import nl.trifox.foxprison.framework.storage.StorageModule;
import nl.trifox.foxprison.modules.economy.EconomyModule;
import nl.trifox.foxprison.modules.ranks.commands.player.RankCommand;
import nl.trifox.foxprison.modules.ranks.commands.player.RankUpCommand;

public final class RankModule implements FoxModule {

    private final FoxPrisonPlugin plugin;
    private final StorageModule storageModule;
    private final EconomyModule economyModule;

    private RankService rankService;

    public RankModule(FoxPrisonPlugin plugin, StorageModule storageModule, EconomyModule economyModule) {
        this.plugin = plugin;
        this.storageModule = storageModule;
        this.economyModule = economyModule;
    }

    @Override
    public void start() {
        var economy = economyModule.getEconomyManager();

        this.rankService = new RankService(storageModule.provider().ranks(), economy, plugin.getRanksConfig());

        plugin.getCommandRegistry().registerCommand(new RankUpCommand(rankService));
        plugin.getCommandRegistry().registerCommand(new RankCommand(rankService));
    }

    @Override
    public void stop() {

    }

    public RankService getRankService() {
        return rankService;
    }
}

