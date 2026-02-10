package nl.trifox.foxprison.modules.sell;

import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import nl.trifox.foxprison.FoxPrisonPlugin;
import nl.trifox.foxprison.api.events.mines.MineDropsEvent;
import nl.trifox.foxprison.framework.module.FoxModule;
import nl.trifox.foxprison.modules.economy.EconomyModule;
import nl.trifox.foxprison.modules.sell.commands.admin.SellAdminCommands;
import nl.trifox.foxprison.modules.sell.commands.player.AutoSellCommand;
import nl.trifox.foxprison.modules.sell.commands.player.SellAllCommand;
import nl.trifox.foxprison.modules.sell.commands.player.SellCommand;
import nl.trifox.foxprison.modules.sell.config.SellConfig;
import nl.trifox.foxprison.modules.sell.listeners.AutoSellEventListener;
import nl.trifox.foxprison.modules.sell.listeners.PlayerEvents;

public class SellModule implements FoxModule {

    private final FoxPrisonPlugin foxPrisonPlugin;
    private final EconomyModule economyModule;
    private final SellConfig sellConfig;
    private PlayerAutoSellService playerAutoSellService;

    public SellModule(FoxPrisonPlugin plugin, EconomyModule economyModule, SellConfig sellConfig)
    {
        this.foxPrisonPlugin = plugin;
        this.economyModule = economyModule;
        this.sellConfig = sellConfig;
    }
    @Override
    public void start() {
        var sellService = new SellService(economyModule.getEconomyManager(), sellConfig);
        playerAutoSellService = new PlayerAutoSellService();
        var autoSellListener = new AutoSellEventListener(sellService, playerAutoSellService);

        if (sellConfig.isSellEnabled()) {
            foxPrisonPlugin.getCommandRegistry().registerCommand(new SellCommand(economyModule.getEconomyManager(), sellConfig));
        }
        if (sellConfig.isSellAllEnabled())
        {
            foxPrisonPlugin.getCommandRegistry().registerCommand(new SellAllCommand(economyModule.getEconomyManager(), sellConfig));
        }

        foxPrisonPlugin.getCommandRegistry().registerCommand(new SellAdminCommands(sellConfig, economyModule.getEconomyManager()));

        if (sellConfig.isAutoSellEnabled()) {
            var playerEvents = new PlayerEvents(playerAutoSellService);
            foxPrisonPlugin.getEventRegistry().register(PlayerDisconnectEvent.class, playerEvents::onPlayerQuit);

            foxPrisonPlugin.getEventRegistry().register(MineDropsEvent.class, autoSellListener::handleMineDrops);
            foxPrisonPlugin.getCommandRegistry().registerCommand(new AutoSellCommand(playerAutoSellService));
        }
    }

    @Override
    public void stop() {

    }
}
