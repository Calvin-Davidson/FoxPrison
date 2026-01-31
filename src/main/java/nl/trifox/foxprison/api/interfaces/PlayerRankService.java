package nl.trifox.foxprison.api.interfaces;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface PlayerRankService {
    CompletableFuture<Integer> getRankIndex(UUID playerId);
    CompletableFuture<String> getRankID(UUID playerId);
}