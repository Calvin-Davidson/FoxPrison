package nl.trifox.foxprison.modules.economy.manager;

import de.fof1092.essentialsplus.economy.EconomyAPI;
import net.milkbowl.vault2.economy.Economy;
import nl.trifox.foxprison.modules.economy.EconomyManager;
import nl.trifox.foxprison.modules.economy.data.LeaderboardEntry;
import nl.trifox.foxprison.modules.economy.enums.TransferResult;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class EssentialsPlusEconomyManager implements EconomyManager {

    public EssentialsPlusEconomyManager() {
    }

    @Override
    public double getBalance(@NotNull UUID playerUuid, String Currency) {
        return EconomyAPI.getBalance(playerUuid).join();
    }

    @Override
    public boolean hasBalance(@NotNull UUID playerUuid, double amount, String Currency) {
        return EconomyAPI.hasBalance(playerUuid, amount).join();
    }

    @Override
    public boolean deposit(@NotNull UUID playerUuid, double amount, String reason, String Currency) {
        EconomyAPI.increaseBalance(playerUuid, amount, reason).join();
        return true;
    }

    @Override
    public boolean withdraw(@NotNull UUID playerUuid, double amount, String reason, String Currency) {
        EconomyAPI.decreaseBalance(playerUuid, amount, reason).join();
        return true;
    }

    @Override
    public void setBalance(@NotNull UUID playerUuid, double amount, String reason, String Currency) {
        EconomyAPI.setBalance(playerUuid, amount, reason).join();
    }

    @Override
    public String format(double amount, String currency) {
        return EconomyAPI.formatCurrency(amount);
    }

    @Override
    public String getCurrencySingular(String currency) {
        return EconomyAPI.getCurrencyNameSingular();
    }

    @Override
    public String getCurrencyPlural(String currency) {
        return EconomyAPI.getCurrencyNamePlural();
    }

    @Override
    public List<LeaderboardEntry> getLeaderboard(int limit, String currency) {
        var top = (EconomyAPI.getTopBalances(1, 10).join());

        return top.entrySet().stream()
                .map(entry -> new LeaderboardEntry(0, entry.getKey(), entry.getValue(), currency))
                .toList();
    }

    @Override
    public TransferResult transfer(@NotNull UUID from, @NotNull UUID to, double amount, String reason, String Currency) {
        if (!EconomyAPI.hasBalance(from, amount).join()) {
            return TransferResult.INSUFFICIENT_FUNDS;
        }

        if (from.equals(to)) {
            return TransferResult.SELF_TRANSFER;
        }

        EconomyAPI.decreaseBalance(from, amount, "transfer").join();
        EconomyAPI.increaseBalance(to, amount, "transfer").join();
        return TransferResult.SUCCESS;
    }

    @Override
    public boolean isAvailable() {
        return EconomyAPI.isEnabled();
    }

    @Override
    public void forceSave() {
        // not supported
    }

    @Override
    public void ensureAccount(UUID uuid) {
        // not supported
    }

    @Override
    public String getDefaultCurrencyID() {
        return EconomyAPI.getCurrencyNameSingular();
    }

    @Override
    public void shutdown() {

    }

    @Override
    public CompletableFuture<Void> ensureAccountAsync(UUID uuid) {
        return null;
    }
}