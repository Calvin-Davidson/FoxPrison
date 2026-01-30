package nl.trifox.foxprison.modules.ranks.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import nl.trifox.foxprison.modules.economy.config.CurrencyDefinition;

public final class RankCostsDefinition {

    public static final Codec<CurrencyCostDefinition[]> CURRENCY_COSTS_ARRAY_CODEC =
            new ArrayCodec<>(CurrencyCostDefinition.CODEC, CurrencyCostDefinition[]::new);

    public static final BuilderCodec<RankCostsDefinition> CODEC =
            BuilderCodec.builder(RankCostsDefinition.class, RankCostsDefinition::new)
                    .append(new KeyedCodec<>("Currency", CURRENCY_COSTS_ARRAY_CODEC),
                            (c, v, i) -> c.currency = (v == null ? new CurrencyCostDefinition[0] : v),
                            (c, i) -> c.currency)
                    .add()
                    .append(new KeyedCodec<>("BlocksMined", Codec.LONG),
                            (c, v, i) -> c.blocksMined = Math.max(0L, v),
                            (c, i) -> c.blocksMined)
                    .add()
                    .build();

    // Multi-currency “spend” costs
    private CurrencyCostDefinition[] currency = new CurrencyCostDefinition[0];

    // Non-currency requirement (not “spent”; just required)
    private long blocksMined = 0L;

    public CurrencyCostDefinition[] getCurrency() {
        return currency == null ? new CurrencyCostDefinition[0] : currency;
    }

    public long getBlocksMined() {
        return blocksMined;
    }

    public double getCurrencyCost(String currencyId) {
        String id = CurrencyDefinition.normalize(currencyId);
        for (CurrencyCostDefinition c : getCurrency()) {
            if (c != null && c.getCurrencyId().equalsIgnoreCase(id)) {
                return c.getAmount();
            }
        }
        return 0.0;
    }

    public boolean hasAnyCurrencyCosts() {
        return getCurrency().length > 0;
    }

    public void ensureDefaultsFromLegacy(double legacyCost, String defaultCurrencyId) {
        if (legacyCost <= 0) return;

        // If new currency costs already exist, don't inject legacy.
        if (hasAnyCurrencyCosts()) return;

        String id = CurrencyDefinition.normalize(defaultCurrencyId);
        this.currency = new CurrencyCostDefinition[] {
                new CurrencyCostDefinition(id, legacyCost)
        };
    }
}