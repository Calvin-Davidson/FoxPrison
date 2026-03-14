package nl.trifox.foxprison.modules.economy.data;

import java.util.UUID;

public record LeaderboardEntry(int rank, UUID playerUuid, double balance, String currencyID) {}