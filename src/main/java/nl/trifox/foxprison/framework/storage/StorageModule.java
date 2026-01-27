package nl.trifox.foxprison.framework.storage;

import nl.trifox.foxprison.FoxPrisonPlugin;
import nl.trifox.foxprison.framework.config.CoreConfig;

public final class StorageModule {

    private final StorageProvider provider;

    public StorageModule(FoxPrisonPlugin plugin, CoreConfig core) {
        this.provider = StorageFactory.create(plugin, core);
    }

    public void start() {
        provider.init();
    }

    public void stop() {
        provider.close();
    }

    public StorageProvider provider() {
        return provider;
    }
}