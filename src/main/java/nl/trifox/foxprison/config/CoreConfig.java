package nl.trifox.foxprison.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class CoreConfig {

    public static final BuilderCodec<CoreConfig> CODEC =
            BuilderCodec.builder(CoreConfig.class, CoreConfig::new)
                    .append(new KeyedCodec<Boolean>("Enabled", Codec.BOOLEAN),
                            (c, v, i) -> c.enabled = v,
                            (c, i) -> c.enabled)
                    .add()
                    .append(new KeyedCodec<String>("DefaultMineId", Codec.STRING),
                            (c, v, i) -> c.defaultMineId = v,
                            (c, i) -> c.defaultMineId)
                    .add()
                    .append(new KeyedCodec<Boolean>("Debug", Codec.BOOLEAN),
                            (c, v, i) -> c.debug = v,
                            (c, i) -> c.debug)
                    .add()
                    .append(new KeyedCodec<String>("DefaultCurrencySymbol", Codec.STRING),
                            (c, v, i) -> c.defaultCurrencySymbol = v,
                            (c, i) -> c.defaultCurrencySymbol)
                    .add()
                    .append(new KeyedCodec<String>("StorageProvider", Codec.STRING),
                            (c, v, i) -> c.storageProvider = v,
                            (c, i) -> c.storageProvider)
                    .add()
                    .append(new KeyedCodec<Boolean>("EnableEconomy", Codec.BOOLEAN),
                            (c, v, i) -> c.enableEconomy = v,
                            (c, i) -> c.enableEconomy)
                    .add()
                    .append(new KeyedCodec<Double>("StartingBalance", Codec.DOUBLE),
                            (c, v, i) -> c.startingBalance = v,
                            (c, i) -> c.startingBalance)
                    .add()
                    .build();

    private boolean enabled = true;
    private String defaultMineId = "a";
    private boolean debug = false;
    private String defaultCurrencySymbol = "€";
    private String storageProvider = "json";
    private boolean enableEconomy = true;
    private double startingBalance;

    public boolean isEnabled() { return enabled; }
    public String getDefaultMineId() { return defaultMineId; }
    public boolean isDebug() { return debug; }

    public String getDefaultCurrencySymbol() {
        return defaultCurrencySymbol;
    }

    public String getStorageProvider() {
        return storageProvider;
    }

    public long getAutoSaveInterval() {
        return 60000; // every minute
    }

    public boolean isEnableEconomy() {
        return enableEconomy;
    }

    public double getStartingBalance() {
        return startingBalance;
    }
}
