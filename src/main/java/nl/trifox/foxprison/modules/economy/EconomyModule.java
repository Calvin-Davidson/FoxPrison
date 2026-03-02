package nl.trifox.foxprison.modules.economy;

import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.common.semver.SemverRange;
import com.hypixel.hytale.logger.HytaleLogger;
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
import nl.trifox.foxprison.modules.economy.hooks.EssentialsPlusHook;
import nl.trifox.foxprison.modules.economy.hooks.VaultUnlockedHook;
import nl.trifox.foxprison.modules.economy.manager.FoxEconomyManager;

public final class EconomyModule implements FoxModule {

    private final FoxPrisonPlugin plugin;
    private final StorageModule storageModule;
    private EconomyManager economyManager;

    public static String VaultGroup = "TheNewEconomy:VaultUnlocked";
    public static String EssentialsGroup = "fof1092:EssentialsPlus";



    public EconomyModule(FoxPrisonPlugin plugin, StorageModule storageModule) {
        this.plugin = plugin;
        this.storageModule = storageModule;
    }

    @Override
    public void start() {
        var pluginManager = HytaleServer.get().getPluginManager();
        if (plugin.getEconomyConfig().get().isEnabled()) {

            // storageModule.start() must have happened before this
            try {
                this.economyManager = new FoxEconomyManager(plugin, storageModule.provider());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            if (pluginManager.hasPlugin(PluginIdentifier.fromString(VaultGroup), SemverRange.WILDCARD)) {
                VaultUnlockedHook.registerProvider();
            } else if (pluginManager.hasPlugin(PluginIdentifier.fromString(EssentialsGroup), SemverRange.WILDCARD)) {
                EssentialsPlusHook.registerProvider();
            }

            var registry = plugin.getCommandRegistry();
            registry.registerCommand(new BalanceCommand());
            registry.registerCommand(new EcoAdminCommand());
            registry.registerCommand(new PayCommand());

            plugin.getEventRegistry().registerGlobal(PlayerReadyEvent.class, PlayerEvents::onPlayerReady);
            plugin.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, PlayerEvents::onPlayerQuit);
        } else {
            if (pluginManager.hasPlugin(PluginIdentifier.fromString(VaultGroup), SemverRange.WILDCARD)) {
                this.economyManager = VaultUnlockedHook.createConsumer();
            } else if (pluginManager.hasPlugin(PluginIdentifier.fromString(EssentialsGroup), SemverRange.WILDCARD)) {
                this.economyManager = EssentialsPlusHook.createConsumer();
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

