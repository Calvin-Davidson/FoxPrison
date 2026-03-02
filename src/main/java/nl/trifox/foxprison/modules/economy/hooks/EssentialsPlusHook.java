package nl.trifox.foxprison.modules.economy.hooks;

import net.cfh.vault.VaultUnlockedServicesManager;
import net.milkbowl.vault2.economy.Economy;
import nl.trifox.foxprison.FoxPrisonPlugin;
import nl.trifox.foxprison.modules.economy.EconomyManager;
import nl.trifox.foxprison.modules.economy.VaultUnlockedEconomy;
import nl.trifox.foxprison.modules.economy.manager.EssentialsPlusEconomyManager;
import nl.trifox.foxprison.modules.economy.manager.VaultUnlockedEconomyManager;

public final class EssentialsPlusHook {

    private EssentialsPlusHook() {}

    public static void registerProvider() {
        FoxPrisonPlugin.getInstance().getLogger().atWarning()
                .log("Tried to register FoxPrison as economy provider for essentials plus, this is not supported");

    }

    public static EconomyManager createConsumer() {
        FoxPrisonPlugin.getInstance().getLogger().atInfo().log("FoxPrison registered as consumer for Essentialsplus");
        return new EssentialsPlusEconomyManager();
    }
}