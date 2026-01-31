package nl.trifox.foxprison.modules.economy.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import java.text.DecimalFormat;
import java.util.Locale;

public class CurrencyDefinition {

    public static final BuilderCodec<CurrencyDefinition> CODEC =
            BuilderCodec.builder(CurrencyDefinition.class, CurrencyDefinition::new)
                    .append(new KeyedCodec<>("Id", Codec.STRING),
                            (c, v, _) -> c.id = v,
                            (c, _) -> c.id)
                    .add()
                    .append(new KeyedCodec<>("DisplayName", Codec.STRING),
                            (c, v, _) -> c.displayName = v,
                            (c, _) -> c.displayName)
                    .add()
                    .append(new KeyedCodec<>("Symbol", Codec.STRING),
                            (c, v, _) -> c.symbol = v,
                            (c, _) -> c.symbol)
                    .add()
                    .append(new KeyedCodec<>("DecimalPlaces", Codec.INTEGER),
                            (c, v, _) -> c.decimalPlaces = v,
                            (c, _) -> c.decimalPlaces)
                    .add()
                    .append(new KeyedCodec<>("StartingBalance", Codec.DOUBLE),
                            (c, v, _) -> c.startingBalance = v,
                            (c, _) -> c.startingBalance)
                    .add()
                    .build();

    private String id = "money";
    private String displayName = "Money";
    private String symbol = "$";
    private int decimalPlaces = 2;
    private double startingBalance;

    public static String normalize(String id) {
        if (id == null) return "money";
        String n = id.trim().toLowerCase(Locale.ROOT);
        return n.isBlank() ? "money" : n;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getDecimalPlaces() {
        return decimalPlaces;
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
        return symbol + df.format(amount);
    }

    /**
     * Format amount in compact form (e.g., "$1.2M", "$500K")
     */
    public String formatShort(double amount) {
        if (amount >= 1_000_000_000) {
            return symbol + String.format("%.1fB", amount / 1_000_000_000);
        } else if (amount >= 1_000_000) {
            return symbol + String.format("%.1fM", amount / 1_000_000);
        } else if (amount >= 10_000) {
            return symbol + String.format("%.1fK", amount / 1_000);
        }
        // Under 10K: show as whole number for cleaner HUD display
        return symbol + Math.round(amount);
    }
}