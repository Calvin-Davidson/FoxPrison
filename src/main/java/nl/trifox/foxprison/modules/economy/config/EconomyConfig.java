package nl.trifox.foxprison.modules.economy.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;

import java.util.*;

public final class EconomyConfig {
    public static final Codec<CurrencyDefinition[]> CURRENCIES_ARRAY_CODEC = new ArrayCodec<>(CurrencyDefinition.CODEC, CurrencyDefinition[]::new, CurrencyDefinition::new);

    private static final CurrencyDefinition DEFAULT_CURRENCY = new CurrencyDefinition() {
    };

    public static final BuilderCodec<EconomyConfig> CODEC =
            BuilderCodec.builder(EconomyConfig.class, EconomyConfig::new)
                    .append(new KeyedCodec<Boolean>("Enabled", Codec.BOOLEAN),
                            (c, v, i) -> c.enabled = v,
                            (c, i) -> c.enabled)
                    .add()
                    .append(new KeyedCodec<String>("DefaultCurrency", Codec.STRING),
                            (c, v, i) -> c.defaultCurrency = v,
                            (c, i) -> c.defaultCurrency)
                    .add()
                    .append(new KeyedCodec<>("Currencies", CURRENCIES_ARRAY_CODEC),
                            (c, v, i) -> c.currencies = (v == null ? new CurrencyDefinition[0] : v),
                            (c, i) -> c.currencies)
                    .add()
                    .build();

    private boolean enabled = true;
    private CurrencyDefinition[] currencies = new CurrencyDefinition[0];
    private String defaultCurrency = "money";

    public boolean isEnabled() {
        return enabled;
    }

    public String getDefaultCurrencyId() {
        return defaultCurrency;
    }

    public CurrencyDefinition[] getCurrencies() {
        if (currencies == null || currencies.length == 0) {
            return new CurrencyDefinition[] {DEFAULT_CURRENCY};
        }
        return currencies;
    }

    public CurrencyDefinition getDefaultCurrency() {
        if (currencies == null || currencies.length == 0) {
            currencies = new CurrencyDefinition[] { DEFAULT_CURRENCY };
        }

        return getCurrency(getDefaultCurrencyId());
    }

    public CurrencyDefinition getCurrency(String currencyId) {
        if (currencies == null || currencies.length == 0) {
            currencies = new CurrencyDefinition[] { DEFAULT_CURRENCY };
        }
        return Arrays.stream(currencies).filter(currencyDefinition -> currencyDefinition.getId().equalsIgnoreCase(CurrencyDefinition.normalize(currencyId))).findFirst().orElseThrow();
    }

    public Set<String> getCurrencyIds() {
        Set<String> ids = new LinkedHashSet<>();
        for (CurrencyDefinition c : currencies) {
            if (c != null) ids.add(c.getId());
        }
        return ids;
    }
}