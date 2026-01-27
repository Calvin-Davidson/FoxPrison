package nl.trifox.foxprison.framework.storage;

import nl.trifox.foxprison.framework.storage.repositories.PlayerBalanceRepository;
import nl.trifox.foxprison.framework.storage.repositories.PlayerRankRepository;

public interface StorageProvider extends AutoCloseable {

    void init();

    PlayerBalanceRepository balances();
    PlayerRankRepository ranks();

    String getName();

    @Override
    void close();
}
