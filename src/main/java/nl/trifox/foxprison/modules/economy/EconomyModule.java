package nl.trifox.foxprison.modules.economy;

import net.cfh.vault.VaultUnlockedServicesManager;
import nl.trifox.foxprison.FoxPrisonPlugin;
import nl.trifox.foxprison.framework.module.FoxModule;
import nl.trifox.foxprison.framework.storage.StorageModule;
import nl.trifox.foxprison.modules.economy.commands.BalanceCommand;
import nl.trifox.foxprison.modules.economy.commands.admin.EcoAdminCommand;
import nl.trifox.foxprison.modules.economy.manager.FoxEconomyManager;
import nl.trifox.foxprison.modules.economy.manager.VaultUnlockedEconomyManager;

public final class EconomyModule implements FoxModule {

    private final FoxPrisonPlugin plugin;
    private final StorageModule storageModule;
    private EconomyManager economyManager;

    public EconomyModule(FoxPrisonPlugin plugin, StorageModule storageModule) {
        this.plugin = plugin;
        this.storageModule = storageModule;
    }

    @Override
    public void start() {
        if (plugin.getEconomyConfig().get().isEnabled()) {

            // storageModule.start() must have happened before this
            try {
                this.economyManager = new FoxEconomyManager(plugin, storageModule.provider());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            // Register into VaultUnlocked AFTER manager exists
            VaultUnlockedServicesManager.get().economy(new VaultUnlockedEconomy(/* ideally pass economyManager */));

            var registry = plugin.getCommandRegistry();
            registry.registerCommand(new BalanceCommand());
            registry.registerCommand(new EcoAdminCommand());
        } else {
            this.economyManager = new VaultUnlockedEconomyManager(VaultUnlockedServicesManager.get().economyObj());
        }
    }

    @Override
    public void stop() {
        economyManager.shutdown();
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }
}

