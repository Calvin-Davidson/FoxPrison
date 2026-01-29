package nl.trifox.foxprison.modules.economy.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import nl.trifox.foxprison.framework.config.CoreConfig;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public final class EconomyConfig {

    public static final BuilderCodec<EconomyConfig> CODEC =
            BuilderCodec.builder(EconomyConfig.class, EconomyConfig::new)
                    .append(new KeyedCodec<Boolean>("Enabled", Codec.BOOLEAN),
                            (c, v, i) -> c.enabled = v,
                            (c, i) -> c.enabled)
                    .add()
                    .append(new KeyedCodec<String>("DefaultCurrencySymbol", Codec.STRING),
                            (c, v, i) -> c.defaultCurrency = v,
                            (c, i) -> c.defaultCurrency)
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


    private final List<CurrencyDefinition> currencies = new ArrayList<>();
    private String defaultCurrency = "";
    private double startingBalance;
    private int decimalPlaces;


    public boolean isEnabled() { return enabled; }
    public String getDefaultCurrency() { return defaultCurrency; }
    public List<CurrencyDefinition> getCurrencies() { return currencies; }


    public String format(double amount) {
        StringBuilder pattern = new StringBuilder("#,##0");
        if (decimalPlaces > 0) {
            pattern.append(".");
            pattern.append("0".repeat(decimalPlaces));
        }
        DecimalFormat df = new DecimalFormat(pattern.toString());
        return defaultCurrency + df.format(amount);
    }

    /**
     * Format amount in compact form (e.g., "$1.2M", "$500K")
     */
    public String formatShort(double amount) {
        if (amount >= 1_000_000_000) {
            return defaultCurrency + String.format("%.1fB", amount / 1_000_000_000);
        } else if (amount >= 1_000_000) {
            return defaultCurrency + String.format("%.1fM", amount / 1_000_000);
        } else if (amount >= 10_000) {
            return defaultCurrency + String.format("%.1fK", amount / 1_000);
        }
        // Under 10K: show as whole number for cleaner HUD display
        return defaultCurrency + Math.round(amount);
    }

    public double getStartingBalance() {
        return startingBalance;
    }
}