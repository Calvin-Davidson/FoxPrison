package nl.trifox.foxprison.framework.storage;

import nl.trifox.foxprison.FoxPrisonPlugin;
import nl.trifox.foxprison.framework.config.CoreConfig;
import nl.trifox.foxprison.framework.module.FoxModule;

public final class StorageModule implements FoxModule {

    private final StorageProvider provider;

    public StorageModule(FoxPrisonPlugin plugin, CoreConfig core) {
        this.provider = StorageFactory.create(plugin, core);
    }

    @Override
    public void start() {
        provider.init();
    }

    @Override
    public void stop() {
        provider.close();
    }

    public StorageProvider provider() {
        return provider;
    }
}