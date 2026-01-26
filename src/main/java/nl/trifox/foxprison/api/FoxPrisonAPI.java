package nl.trifox.foxprison.api;

import nl.trifox.foxprison.economy.EconomyManager;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public final class FoxPrisonAPI {
    public static EconomyManager economyManager;

    public static BigDecimal getBalance(@NotNull UUID accountID) {
        return BigDecimal.valueOf(economyManager.getBalance(accountID));
    }

    public static boolean hasBalance(@NotNull UUID accountID, double v) {
        return economyManager.hasBalance(accountID, v);
    }

    public static boolean withdraw(@NotNull UUID accountID, double v, @NotNull String s) {
        return economyManager.withdraw(accountID, v, s);
    }

    public static boolean deposit(@NotNull UUID accountID, double v, @NotNull String s) {
        return economyManager.deposit(accountID, v, s);
    }
}
