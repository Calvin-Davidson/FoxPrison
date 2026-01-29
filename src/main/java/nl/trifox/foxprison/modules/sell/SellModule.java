package nl.trifox.foxprison.modules.sell;

import nl.trifox.foxprison.FoxPrisonPlugin;
import nl.trifox.foxprison.framework.module.FoxModule;
import nl.trifox.foxprison.modules.economy.EconomyModule;
import nl.trifox.foxprison.modules.sell.commands.admin.SellAdminCommands;
import nl.trifox.foxprison.modules.sell.commands.player.SellAllCommand;
import nl.trifox.foxprison.modules.sell.commands.player.SellCommand;
import nl.trifox.foxprison.modules.sell.config.SellConfig;

public class SellModule implements FoxModule {

    private final FoxPrisonPlugin foxPrisonPlugin;
    private final EconomyModule economyModule;
    private final SellConfig sellConfig;

    public SellModule(FoxPrisonPlugin plugin, EconomyModule economyModule, SellConfig sellConfig)
    {
        this.foxPrisonPlugin = plugin;
        this.economyModule = economyModule;
        this.sellConfig = sellConfig;
    }
    @Override
    public void start() {
        if (sellConfig.isSellEnabled()) {
            foxPrisonPlugin.getCommandRegistry().registerCommand(new SellCommand(economyModule.getEconomyManager(), sellConfig));
        }
        if (sellConfig.isSellAllEnabled())
        {
            foxPrisonPlugin.getCommandRegistry().registerCommand(new SellAllCommand(economyModule.getEconomyManager(), sellConfig));
        }

        foxPrisonPlugin.getCommandRegistry().registerCommand(new SellAdminCommands(sellConfig, economyModule.getEconomyManager()));
    }

    @Override
    public void stop() {

    }
}
