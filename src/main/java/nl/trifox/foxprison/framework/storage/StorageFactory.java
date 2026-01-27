package nl.trifox.foxprison.framework.storage;

import nl.trifox.foxprison.FoxPrisonPlugin;
import nl.trifox.foxprison.framework.config.CoreConfig;
import nl.trifox.foxprison.framework.config.StorageProviderType;
import nl.trifox.foxprison.framework.storage.json.JsonStorageProvider;

import java.io.File;

public final class StorageFactory {

    private StorageFactory() {}

    public static StorageProvider create(FoxPrisonPlugin plugin, CoreConfig core) {
        String raw = core.getStorageProvider();

        StorageProviderType type = java.util.Arrays.stream(StorageProviderType.values())
                .filter(v -> v.name().equalsIgnoreCase(raw))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown storage provider: " + raw + " (expected one of: " +
                                java.util.Arrays.toString(StorageProviderType.values()) + ")"
                ));

        if (type == StorageProviderType.JSON) {
            File base = new File(plugin.getDataDirectory().toFile(), "storage/json"); // adjust if your API differs
            return new JsonStorageProvider(plugin, base);
        }

        // later:
        // if (type == StorageProviderType.MYSQL) return new MySqlStorageProvider(...);

        throw new IllegalStateException("Unsupported storage provider: " + type);
    }
}