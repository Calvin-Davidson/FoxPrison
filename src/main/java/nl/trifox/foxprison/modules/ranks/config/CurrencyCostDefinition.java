package nl.trifox.foxprison.modules.ranks.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import nl.trifox.foxprison.modules.economy.config.CurrencyDefinition;

public final class CurrencyCostDefinition {

    public static final BuilderCodec<CurrencyCostDefinition> CODEC =
            BuilderCodec.builder(CurrencyCostDefinition.class, CurrencyCostDefinition::new)
                    .append(new KeyedCodec<>("CurrencyId", Codec.STRING),
                            (c, v, i) -> c.currencyId = CurrencyDefinition.normalize(v),
                            (c, i) -> c.currencyId)
                    .add()
                    .append(new KeyedCodec<>("Amount", Codec.DOUBLE),
                            (c, v, i) -> c.amount = Math.max(0.0, v),
                            (c, i) -> c.amount)
                    .add()
                    .build();

    private String currencyId = "money";
    private double amount = 0.0;

    public CurrencyCostDefinition() {}

    public CurrencyCostDefinition(String currencyId, double amount) {
        this.currencyId = CurrencyDefinition.normalize(currencyId);
        this.amount = Math.max(0.0, amount);
    }

    public String getCurrencyId() { return currencyId; }
    public double getAmount() { return amount; }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}