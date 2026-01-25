package nl.trifox.foxprison.economy;

import net.cfh.vault.VaultUnlockedServicesManager;
import nl.trifox.foxprison.FoxPrisonPlugin;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Adapter stub for TheEconomy.
 * Replace TODOs with real TheEconomy API calls.
 */
public class TheEconomyAdapter implements Economy {

    private final FoxPrisonPlugin plugin;
    private final net.milkbowl.vault2.economy.Economy api;

    public TheEconomyAdapter(FoxPrisonPlugin plugin) {
        this.plugin = plugin;
        this.api = VaultUnlockedServicesManager.get().economyObj();
    }

    @Override
    public boolean isAvailable() {
        return api != null;
    }

    @Override
    public BigDecimal getBalance(UUID playerId) {
        return api.balance(plugin.getName(), playerId);
    }

    @Override
    public boolean withdraw(UUID playerId, BigDecimal amount) {
        return api.withdraw(plugin.getName(), playerId, amount).transactionSuccess();
    }

    @Override
    public void deposit(UUID playerId, BigDecimal amount) {
        api.deposit(plugin.getName(), playerId, amount);
    }
}
