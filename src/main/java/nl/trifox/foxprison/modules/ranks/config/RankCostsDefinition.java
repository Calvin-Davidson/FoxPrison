package nl.trifox.foxprison.modules.ranks.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import nl.trifox.foxprison.modules.economy.config.CurrencyDefinition;
import nl.trifox.foxprison.modules.mines.config.MineDefinition;
import nl.trifox.foxprison.modules.mines.config.MinesConfig;

import java.util.Arrays;

public final class RankCostsDefinition {

    public static final Codec<CurrencyCostDefinition[]> CURRENCY_COSTS_ARRAY_CODEC =
            new ArrayCodec<>(CurrencyCostDefinition.CODEC, CurrencyCostDefinition[]::new);

    public static final BuilderCodec<RankCostsDefinition> CODEC =
            BuilderCodec.builder(RankCostsDefinition.class, RankCostsDefinition::new)
                    .append(new KeyedCodec<>("Currency", CURRENCY_COSTS_ARRAY_CODEC),
                            (c, v, i) -> c.currencies = (v == null ? new CurrencyCostDefinition[0] : v),
                            (c, i) -> c.currencies)
                    .add()
                    .append(new KeyedCodec<>("BlocksMined", Codec.LONG),
                            (c, v, i) -> c.blocksMined = Math.max(0L, v),
                            (c, i) -> c.blocksMined)
                    .add()
                    .build();

    // Multi-currency “spend” costs
    private CurrencyCostDefinition[] currencies = new CurrencyCostDefinition[0];

    // Non-currency requirement (not “spent”; just required)
    private long blocksMined = 0L;

    public CurrencyCostDefinition[] getCurrencies() {
        return currencies == null ? new CurrencyCostDefinition[0] : currencies;
    }

    public long getBlocksMined() {
        return blocksMined;
    }

    public double getCurrencyCost(String currencyId) {
        String id = CurrencyDefinition.normalize(currencyId);
        for (CurrencyCostDefinition c : getCurrencies()) {
            if (c != null && c.getCurrencyId().equalsIgnoreCase(id)) {
                return c.getAmount();
            }
        }
        return 0.0;
    }

    public void setCurrencyCost(String currencyId, double amount) {
        var existing = Arrays.stream(currencies).filter(currencyCostDefinition -> currencyCostDefinition.getCurrencyId().equalsIgnoreCase(currencyId)).findFirst();
        if (existing.isEmpty()) {
            CurrencyCostDefinition[] oldArr = currencies;
            CurrencyCostDefinition[] newArr = java.util.Arrays.copyOf(oldArr, oldArr.length + 1);
            newArr[newArr.length - 1] = new CurrencyCostDefinition(currencyId, amount);
            currencies = newArr;
        } else {
            existing.get().setAmount(amount);
        }
    }

    public boolean hasAnyCurrencyCosts() {
        return getCurrencies().length > 0;
    }
}