package nl.trifox.foxprison.modules.economy;

import nl.trifox.foxprison.modules.economy.data.LeaderboardEntry;
import nl.trifox.foxprison.modules.economy.data.PlayerBalanceData;
import nl.trifox.foxprison.modules.economy.enums.TransferResult;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface EconomyManager {

    default double getBalance(@Nonnull UUID playerUuid) {
        return getBalance(playerUuid, getDefaultCurrencyID());
    }
    double getBalance(@Nonnull UUID playerUuid, String Currency);

    default boolean hasBalance(@Nonnull UUID playerUuid, double amount) {
        return hasBalance(playerUuid, amount, getDefaultCurrencyID());
    }
    boolean hasBalance(@Nonnull UUID playerUuid, double amount, String Currency);


    default boolean deposit(@Nonnull UUID playerUuid, double amount, String reason) {
        return deposit(playerUuid, amount, reason, getDefaultCurrencyID());
    }
    boolean deposit(@Nonnull UUID playerUuid, double amount, String reason, String Currency);

    default boolean withdraw(@Nonnull UUID playerUuid, double amount, String reason) {
        return withdraw(playerUuid, amount, reason, getDefaultCurrencyID());
    }
    boolean withdraw(@Nonnull UUID playerUuid, double amount, String reason, String Currency);

    default void setBalance(@Nonnull UUID playerUuid, double amount, String reason) {
        setBalance(playerUuid, amount, reason, getDefaultCurrencyID());
    }
    void setBalance(@Nonnull UUID playerUuid, double amount, String reason, String Currency);

    List<LeaderboardEntry> getLeaderboard(int limit, String currency);

    default List<LeaderboardEntry> getLeaderboard(int limit) {
        return getLeaderboard(limit, getDefaultCurrencyID());
    }

    default TransferResult transfer(@Nonnull UUID from, @Nonnull UUID to, double amount, String reason) {
        return transfer(from, to, amount, reason, getDefaultCurrencyID());
    }

    TransferResult transfer(@Nonnull UUID from, @Nonnull UUID to, double amount, String reason, String Currency);

    String format(double amount, String currency);

    boolean isAvailable();

    void forceSave();

    void ensureAccount(UUID uuid);

    String getDefaultCurrencyID();

    void shutdown();

    CompletableFuture<Void> ensureAccountAsync(UUID uuid);

    String getCurrencySingular(String currency);
    String getCurrencyPlural(String currency);


}