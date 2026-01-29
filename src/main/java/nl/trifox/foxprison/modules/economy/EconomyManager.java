package nl.trifox.foxprison.modules.economy;

import nl.trifox.foxprison.modules.economy.data.PlayerBalanceData;
import nl.trifox.foxprison.modules.economy.enums.TransferResult;

import javax.annotation.Nonnull;
import java.util.UUID;

public interface EconomyManager {

    double getBalance(@Nonnull UUID playerUuid);
    boolean hasBalance(@Nonnull UUID playerUuid, double amount);
    boolean deposit(@Nonnull UUID playerUuid, double amount, String reason);
    boolean withdraw(@Nonnull UUID playerUuid, double amount, String reason);
    void setBalance(@Nonnull UUID playerUuid, double amount, String reason);

    TransferResult transfer(@Nonnull UUID from, @Nonnull UUID to, double amount, String reason);

    boolean isAvailable();

    void forceSave();

    void ensureAccount(UUID uuid);

    String getDefaultCurrencyID();
}