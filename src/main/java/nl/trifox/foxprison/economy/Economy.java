package nl.trifox.foxprison.economy;

import java.util.UUID;

public interface Economy {
    boolean isAvailable();
    double getBalance(UUID playerId);
    boolean withdraw(UUID playerId, double amount);
    void deposit(UUID playerId, double amount);
}
