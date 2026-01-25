package nl.trifox.foxprison.economy;

import java.math.BigDecimal;
import java.util.UUID;

public interface Economy {
    boolean isAvailable();
    BigDecimal getBalance(UUID playerId);
    boolean withdraw(UUID playerId, BigDecimal amount);
    void deposit(UUID playerId, BigDecimal amount);
}
