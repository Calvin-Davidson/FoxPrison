package nl.trifox.foxprison.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class EconomyConfig {

    public static final BuilderCodec<EconomyConfig> CODEC =
            BuilderCodec.builder(EconomyConfig.class, EconomyConfig::new)
                    .append(new KeyedCodec<String>("Provider", Codec.STRING),
                            (c, v, i) -> c.provider = v,
                            (c, i) -> c.provider)
                    .add()
                    .append(new KeyedCodec<Double>("SellMultiplier", Codec.DOUBLE),
                            (c, v, i) -> c.sellMultiplier = v,
                            (c, i) -> c.sellMultiplier)
                    .add()
                    .build();

    // Start with "TheEconomy" as you planned
    private String provider = "TheEconomy";
    private double sellMultiplier = 1.0;

    public String getProvider() { return provider; }
    public double getSellMultiplier() { return sellMultiplier; }
}
