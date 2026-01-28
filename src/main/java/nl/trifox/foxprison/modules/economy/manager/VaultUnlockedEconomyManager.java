package nl.trifox.foxprison.modules.economy.manager;

import net.milkbowl.vault2.economy.Economy;
import nl.trifox.foxprison.FoxPrisonPlugin;
import nl.trifox.foxprison.modules.economy.EconomyManager;
import nl.trifox.foxprison.modules.economy.enums.TransferResult;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public class VaultUnlockedEconomyManager implements EconomyManager {

    private final Economy economy;
    private final String pluginName;

    public VaultUnlockedEconomyManager(Economy economy) {
        this.economy = economy;
        pluginName = FoxPrisonPlugin.getInstance().getName();
    }

    @Override
    public double getBalance(@NotNull UUID playerUuid) {
        return economy.balance(pluginName, playerUuid).doubleValue();
    }

    @Override
    public boolean hasBalance(@NotNull UUID playerUuid, double amount) {
        return economy.has(pluginName, playerUuid, BigDecimal.valueOf(amount));
    }

    @Override
    public boolean deposit(@NotNull UUID playerUuid, double amount, String reason) {
        return economy.deposit(pluginName, playerUuid, BigDecimal.valueOf(amount)).transactionSuccess();
    }

    @Override
    public boolean withdraw(@NotNull UUID playerUuid, double amount, String reason) {
        return economy.withdraw(pluginName, playerUuid, BigDecimal.valueOf(amount)).transactionSuccess();
    }

    @Override
    public void setBalance(@NotNull UUID playerUuid, double amount, String reason) {
        economy.set(pluginName, playerUuid, BigDecimal.valueOf(amount));
    }

    @Override
    public TransferResult transfer(@NotNull UUID from, @NotNull UUID to, double amount, String reason) {
        if (!economy.has(pluginName, from, BigDecimal.valueOf(amount))) {
            return TransferResult.INSUFFICIENT_FUNDS;
        }

        if (from.equals(to)) {
            return TransferResult.SELF_TRANSFER;
        }

        economy.withdraw(pluginName, from, BigDecimal.valueOf(amount));
        economy.deposit(pluginName, to, BigDecimal.valueOf(amount));
        return TransferResult.SUCCESS;
    }

    @Override
    public boolean isAvailable() {
        return economy != null;
    }

    @Override
    public void forceSave() {
        // not supported
    }

    @Override
    public void ensureAccount(UUID uuid) {
        if (!economy.hasAccount(uuid)) {
            economy.createAccount(uuid, pluginName, true);
        }
    }
}
