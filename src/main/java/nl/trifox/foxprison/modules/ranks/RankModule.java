package nl.trifox.foxprison.modules.ranks;

import nl.trifox.foxprison.FoxPrisonPlugin;
import nl.trifox.foxprison.framework.module.FoxModule;
import nl.trifox.foxprison.framework.storage.StorageModule;
import nl.trifox.foxprison.modules.economy.EconomyModule;
import nl.trifox.foxprison.modules.ranks.commands.admin.RankCommands;
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
        // If economy is disabled, decide what RankService should do:
        // - either allow rankups without money,
        // - or block rankups,
        // - or use a NullEconomy implementation.
        var economy = economyModule.getEconomyManager();

        this.rankService = new RankService(storageModule.provider().ranks(), economy, plugin.getRanksConfig());

        // NOTE: these commands depend on MineService too — so either:
        // 1) register them later, or
        // 2) allow MineModule to register RankUpCommand after both exist.
        // I'll show the cleaner approach below.
    }

    public RankService getRankService() {
        return rankService;
    }
}

