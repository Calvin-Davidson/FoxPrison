package nl.trifox.foxprison.economy;

import nl.trifox.foxprison.FoxPrisonPlugin;

import java.util.UUID;

/**
 * Adapter stub for TheEconomy.
 * Replace TODOs with real TheEconomy API calls.
 */
public class TheEconomyAdapter implements Economy {

    private final FoxPrisonPlugin plugin;

    public TheEconomyAdapter(FoxPrisonPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean isAvailable() {
        // TODO: detect TheEconomy plugin / service once you wire the real API.
        return true;
    }

    @Override
    public double getBalance(UUID playerId) {
        // TODO: TheEconomy.getBalance(playerId)
        return 0;
    }

    @Override
    public boolean withdraw(UUID playerId, double amount) {
        // TODO: return TheEconomy.withdraw(playerId, amount).success()
        return true;
    }

    @Override
    public void deposit(UUID playerId, double amount) {
        // TODO: TheEconomy.deposit(playerId, amount)
    }
}
