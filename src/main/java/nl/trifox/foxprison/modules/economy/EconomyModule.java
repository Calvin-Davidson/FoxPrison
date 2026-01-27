package nl.trifox.foxprison.modules.economy;

import net.cfh.vault.VaultUnlockedServicesManager;
import nl.trifox.foxprison.FoxPrisonPlugin;
import nl.trifox.foxprison.modules.economy.commands.BalanceCommand;
import nl.trifox.foxprison.modules.economy.commands.admin.EcoAdminCommand;

public class EconomyModule {

    private EconomyManager economyManager;

    public EconomyModule() {
        FoxPrisonPlugin foxPrisonPlugin = FoxPrisonPlugin.getInstance();
        if (foxPrisonPlugin.getCoreConfig().get().isEconomyEnabled()) {
            try {
                this.economyManager = new EconomyManager(foxPrisonPlugin);
                if (foxPrisonPlugin.getCoreConfig().get().isEconomyEnabled()) {
                    VaultUnlockedServicesManager.get().economy(new VaultUnlockedEconomy());
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            var registry = foxPrisonPlugin.getCommandRegistry();
            registry.registerCommand(new BalanceCommand());
            registry.registerCommand(new EcoAdminCommand());
        }
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }
}
