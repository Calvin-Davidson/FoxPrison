package nl.trifox.foxprison.framework.storage;

import nl.trifox.foxprison.FoxPrisonPlugin;
import nl.trifox.foxprison.framework.config.CoreConfig;
import nl.trifox.foxprison.framework.config.StorageProviderType;
import nl.trifox.foxprison.framework.storage.json.JsonStorageProvider;

import java.io.File;

public final class StorageFactory {

    private StorageFactory() {}

    public static StorageProvider create(FoxPrisonPlugin plugin, CoreConfig core) {
        StorageProviderType type = Enum.valueOf(StorageProviderType.class, core.getStorageProvider());

        if (type == StorageProviderType.JSON) {
            File base = new File(plugin.getDataDirectory().toFile(), "storage/json"); // adjust if your API differs
            return new JsonStorageProvider(plugin, base);
        }

        // later:
        // if (type == StorageProviderType.MYSQL) return new MySqlStorageProvider(...);

        throw new IllegalStateException("Unsupported storage provider: " + type);
    }
}