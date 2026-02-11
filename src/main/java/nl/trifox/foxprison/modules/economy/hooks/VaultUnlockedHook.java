package nl.trifox.foxprison.modules.economy.hooks;

import net.cfh.vault.VaultUnlockedServicesManager;
import net.milkbowl.vault2.economy.Economy;
import nl.trifox.foxprison.FoxPrisonPlugin;
import nl.trifox.foxprison.modules.economy.EconomyManager;
import nl.trifox.foxprison.modules.economy.VaultUnlockedEconomy;
import nl.trifox.foxprison.modules.economy.manager.VaultUnlockedEconomyManager;

public final class VaultUnlockedHook {

    private VaultUnlockedHook() {}

    public static void registerProvider() {
        Economy impl = new VaultUnlockedEconomy();
        VaultUnlockedServicesManager.get().economy(impl);
        FoxPrisonPlugin.getInstance().getLogger().atInfo().log("Registered FoxPrison as economy provider for VaultUnlocked");
    }

    public static EconomyManager createConsumer() {
        Economy eco = VaultUnlockedServicesManager.get().economyObj();
        FoxPrisonPlugin.getInstance().getLogger().atInfo().log("FoxPrison registered as consumer for VaultUnlocked");
        return new VaultUnlockedEconomyManager(eco);
    }
}