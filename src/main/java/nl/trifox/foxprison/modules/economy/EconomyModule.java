package nl.trifox.foxprison.modules.economy;

import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.common.semver.SemverRange;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import nl.trifox.foxprison.FoxPrisonPlugin;
import nl.trifox.foxprison.framework.module.FoxModule;
import nl.trifox.foxprison.framework.storage.StorageModule;
import nl.trifox.foxprison.modules.economy.command.player.BalanceCommand;
import nl.trifox.foxprison.modules.economy.command.admin.EcoAdminCommand;
import nl.trifox.foxprison.modules.economy.command.player.PayCommand;
import nl.trifox.foxprison.modules.economy.event.PlayerEvents;
import nl.trifox.foxprison.modules.economy.hooks.VaultUnlockedHook;
import nl.trifox.foxprison.modules.economy.manager.FoxEconomyManager;

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

            if (HytaleServer.get().getPluginManager().hasPlugin(PluginIdentifier.fromString("TheNewEconomy:VaultUnlocked"), SemverRange.WILDCARD)) {
                VaultUnlockedHook.registerProvider();
            }

            var registry = plugin.getCommandRegistry();
            registry.registerCommand(new BalanceCommand());
            registry.registerCommand(new EcoAdminCommand());
            registry.registerCommand(new PayCommand());

            plugin.getEventRegistry().registerGlobal(PlayerReadyEvent.class, PlayerEvents::onPlayerReady);
            plugin.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, PlayerEvents::onPlayerQuit);
        } else {
            if (HytaleServer.get().getPluginManager().hasPlugin(PluginIdentifier.fromString("TheNewEconomy:VaultUnlocked"), SemverRange.WILDCARD)) {
                this.economyManager = VaultUnlockedHook.createConsumer();
            }
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

