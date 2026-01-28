package nl.trifox.foxprison.modules.sell.config;


import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public final class SellPriceDefinition {

    public static final BuilderCodec<SellPriceDefinition> CODEC =
            BuilderCodec.builder(SellPriceDefinition.class, SellPriceDefinition::new)

                    .append(new KeyedCodec<>("PriceEach", Codec.DOUBLE),
                            (cfg, v) -> cfg.priceEach = v,
                            cfg -> cfg.priceEach)
                    .add()

                    // Optional override; if blank/null, use SellConfig.DefaultCurrency
                    .append(new KeyedCodec<>("Currency", Codec.STRING),
                            (cfg, v) -> cfg.currency = v,
                            cfg -> cfg.currency)
                    .add()

                    .append(new KeyedCodec<>("AllowSell", Codec.BOOLEAN),
                            (cfg, v) -> cfg.allowSell = v,
                            cfg -> cfg.allowSell)
                    .add()

                    .append(new KeyedCodec<>("AllowSellAll", Codec.BOOLEAN),
                            (cfg, v) -> cfg.allowSellAll = v,
                            cfg -> cfg.allowSellAll)
                    .add()

                    .build();

    private double priceEach = 0.0;
    private String currency = ""; // empty = use default currency
    private boolean allowSell = true;
    private boolean allowSellAll = true;

    public double getPriceEach() { return priceEach; }
    public String getCurrency() { return currency; }
    public boolean isAllowSell() { return allowSell; }
    public boolean isAllowSellAll() { return allowSellAll; }

    public String resolveCurrency(String defaultCurrency) {
        if (currency == null || currency.isBlank()) return defaultCurrency;
        return currency;
    }
}