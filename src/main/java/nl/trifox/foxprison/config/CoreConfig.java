package nl.trifox.foxprison.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import java.text.DecimalFormat;

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
                            (c, v, i) -> c.economyEnabled = v,
                            (c, i) -> c.economyEnabled)
                    .add()
                    .append(new KeyedCodec<Double>("StartingBalance", Codec.DOUBLE),
                            (c, v, i) -> c.startingBalance = v,
                            (c, i) -> c.startingBalance)
                    .add()
                    .append(new KeyedCodec<Integer>("CurrencyDecimalPlaces", Codec.INTEGER),
                            (c, v, i) -> c.decimalPlaces = v,
                            (c, i) -> c.decimalPlaces)
                    .add()
                    .build();

    private boolean enabled = true;
    private String defaultMineId = "a";
    private boolean debug = false;
    private String defaultCurrencySymbol = "";
    private String storageProvider = "json";
    private boolean economyEnabled = true;
    private double startingBalance;
    private int decimalPlaces;

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

    public boolean isEconomyEnabled() {
        return economyEnabled;
    }

    public double getStartingBalance() {
        return startingBalance;
    }

    public String format(double amount) {
        StringBuilder pattern = new StringBuilder("#,##0");
        if (decimalPlaces > 0) {
            pattern.append(".");
            pattern.append("0".repeat(decimalPlaces));
        }
        DecimalFormat df = new DecimalFormat(pattern.toString());
        return defaultCurrencySymbol + df.format(amount);
    }

    /**
     * Format amount in compact form (e.g., "$1.2M", "$500K")
     */
    public String formatShort(double amount) {
        if (amount >= 1_000_000_000) {
            return defaultCurrencySymbol + String.format("%.1fB", amount / 1_000_000_000);
        } else if (amount >= 1_000_000) {
            return defaultCurrencySymbol + String.format("%.1fM", amount / 1_000_000);
        } else if (amount >= 10_000) {
            return defaultCurrencySymbol + String.format("%.1fK", amount / 1_000);
        }
        // Under 10K: show as whole number for cleaner HUD display
        return defaultCurrencySymbol + Math.round(amount);
    }
}
