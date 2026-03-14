package nl.trifox.foxprison.modules.economy.manager;

import net.milkbowl.vault2.economy.Economy;
import nl.trifox.foxprison.FoxPrisonPlugin;
import nl.trifox.foxprison.modules.economy.EconomyManager;
import nl.trifox.foxprison.modules.economy.data.LeaderboardEntry;
import nl.trifox.foxprison.modules.economy.enums.TransferResult;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class VaultUnlockedEconomyManager implements EconomyManager {

    private final Economy economy;
    private final String pluginName;

    public VaultUnlockedEconomyManager(Economy economy) {
        this.economy = economy;
        pluginName = FoxPrisonPlugin.getInstance().getName();
    }

    @Override
    public double getBalance(@NotNull UUID playerUuid, String currency) {
        return economy.balance(pluginName, playerUuid, "", currency).doubleValue();
    }

    @Override
    public boolean hasBalance(@NotNull UUID playerUuid, double amount, String currency) {
        return getBalance(playerUuid, currency) >= amount;
    }

    @Override
    public boolean deposit(@NotNull UUID playerUuid, double amount, String reason, String currency) {
        return economy.deposit(pluginName, playerUuid, "", currency, BigDecimal.valueOf(amount)).transactionSuccess();
    }

    @Override
    public boolean withdraw(@NotNull UUID playerUuid, double amount, String reason, String currency) {
        return economy.withdraw(pluginName, playerUuid, "", currency, BigDecimal.valueOf(amount)).transactionSuccess();
    }

    @Override
    public void setBalance(@NotNull UUID playerUuid, double amount, String reason, String currency) {
        economy.set(pluginName, playerUuid, "", currency, BigDecimal.valueOf(amount)).transactionSuccess();
    }

    @Override
    public String format(double amount, String currency) {
        return economy.format(currency, BigDecimal.valueOf(amount));
    }

    @Override
    public TransferResult transfer(@NotNull UUID from, @NotNull UUID to, double amount, String reason, String currency) {
        return null;
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

    @Override
    public String getDefaultCurrencyID() {
        return economy.getDefaultCurrency(pluginName);
    }

    @Override
    public void shutdown() {
        // not supported
    }

    @Override
    public CompletableFuture<Void> ensureAccountAsync(UUID uuid) {
        ensureAccount(uuid); // no async needed
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public String getCurrencySingular(String currency) {
        return economy.defaultCurrencyNameSingular(pluginName);
    }

    @Override
    public String getCurrencyPlural(String currency) {
        return economy.defaultCurrencyNamePlural(pluginName);
    }

    @Override
    public List<LeaderboardEntry> getLeaderboard(int limit, String currency) {
        return economy.getUUIDNameMap().keySet().stream()
                .map(uuid -> new LeaderboardEntry(0, uuid, getBalance(uuid, currency), currency))
                .sorted((a, b) -> Double.compare(b.balance(), a.balance()))
                .limit(limit)
                .toList();
    }
}
