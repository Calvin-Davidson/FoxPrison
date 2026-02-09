package nl.trifox.foxprison.modules.ranks;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import nl.trifox.foxprison.FoxPrisonPlugin;
import nl.trifox.foxprison.framework.module.FoxModule;
import nl.trifox.foxprison.framework.storage.StorageModule;
import nl.trifox.foxprison.modules.economy.EconomyModule;
import nl.trifox.foxprison.modules.ranks.command.player.RankCommand;
import nl.trifox.foxprison.modules.ranks.command.player.RankUpCommand;
import nl.trifox.foxprison.modules.ranks.event.PlayerEvents;

public final class RankModule implements FoxModule {

    private final FoxPrisonPlugin plugin;

    private RankService rankService;

    public RankModule(FoxPrisonPlugin plugin, StorageModule storageModule, EconomyModule economyModule) {
        this.plugin = plugin;

        var economy = economyModule.getEconomyManager();
        this.rankService = new RankService(storageModule.provider().ranks(), economy, plugin.getRanksConfig());
    }

    @Override
    public void start() {
        plugin.getCommandRegistry().registerCommand(new RankUpCommand(rankService));
        plugin.getCommandRegistry().registerCommand(new RankCommand(rankService));

        plugin.getEventRegistry().registerGlobal(PlayerReadyEvent.class, PlayerEvents::onPlayerReady);
        plugin.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, PlayerEvents::onPlayerQuit);
    }

    @Override
    public void stop() {

    }

    public RankService getRankService() {
        return rankService;
    }
}


