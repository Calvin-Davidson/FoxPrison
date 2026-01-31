package nl.trifox.foxprison.framework.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class CoreConfig {

    public static final BuilderCodec<CoreConfig> CODEC =
            BuilderCodec.builder(CoreConfig.class, CoreConfig::new)
                    .append(new KeyedCodec<>("Enabled", Codec.BOOLEAN),
                            (c, v, _) -> c.enabled = v,
                            (c, _) -> c.enabled)
                    .add()
                    .append(new KeyedCodec<>("DefaultMineId", Codec.STRING),
                            (c, v, _) -> c.defaultMineId = v,
                            (c, _) -> c.defaultMineId)
                    .add()
                    .append(new KeyedCodec<>("StorageProvider", Codec.STRING),
                            (c, v, _) -> c.storageProvider = v,
                            (c, _) -> c.storageProvider)
                    .add()
                    .build();

    public boolean enabled = true;
    private String defaultMineId = "a";

    private String storageProvider = "json";

    public boolean isEnabled() { return enabled; }
    public String getDefaultMineId() { return defaultMineId; }

    public String getStorageProvider() {
        return storageProvider;
    }

    public long getAutoSaveInterval() {
        return 60; // every minute
    }
}
