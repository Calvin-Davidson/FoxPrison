package nl.trifox.foxprison.modules.economy;

import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import net.cfh.vault.VaultUnlockedServicesManager;
import nl.trifox.foxprison.FoxPrisonPlugin;
import nl.trifox.foxprison.framework.module.FoxModule;
import nl.trifox.foxprison.framework.storage.StorageModule;
import nl.trifox.foxprison.modules.economy.command.player.BalanceCommand;
import nl.trifox.foxprison.modules.economy.command.admin.EcoAdminCommand;
import nl.trifox.foxprison.modules.economy.command.player.PayCommand;
import nl.trifox.foxprison.modules.economy.event.PlayerEvents;
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
            registry.registerCommand(new PayCommand());

            plugin.getEventRegistry().registerGlobal(PlayerReadyEvent.class, PlayerEvents::onPlayerReady);
            plugin.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, PlayerEvents::onPlayerQuit);
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

