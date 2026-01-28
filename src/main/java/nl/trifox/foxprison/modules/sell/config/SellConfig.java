package nl.trifox.foxprison.modules.sell.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import nl.trifox.foxprison.modules.ranks.config.RankDefinition;

import java.util.*;

/**
 * Sell module config for /sell and /sellall.
 *
 * Suggested JSON file: Sell.json (or FoxPrison/Sell.json depending on your config loader)
 */
public final class SellConfig {

    public static final BuilderCodec<SellConfig> CODEC =
            BuilderCodec.builder(SellConfig.class, SellConfig::new)

                    .append(new KeyedCodec<>("Enabled", Codec.BOOLEAN),
                            (cfg, v) -> cfg.enabled = v,
                            cfg -> cfg.enabled)
                    .add()

                    .append(new KeyedCodec<>("SellEnabled", Codec.BOOLEAN),
                            (cfg, v) -> cfg.sellEnabled = v,
                            cfg -> cfg.sellEnabled)
                    .add()

                    .append(new KeyedCodec<>("SellAllEnabled", Codec.BOOLEAN),
                            (cfg, v) -> cfg.sellAllEnabled = v,
                            cfg -> cfg.sellAllEnabled)
                    .add()
                    .append(new KeyedCodec<>("SellAll", SellAllDefinition.CODEC),
                            (cfg, v) -> cfg.sellAll = (v == null ? new SellAllDefinition() : v),
                            cfg -> cfg.sellAll)
                    .add()
                    .append(new KeyedCodec<>("Prices", new MapCodec<>(SellPriceDefinition.CODEC, HashMap::new, false)),
                            (cfg, v) -> cfg.prices = (v == null ? new HashMap<>() : v),
                            cfg -> cfg.prices)
                    .add()
                    .build();

    private boolean enabled = true;
    private boolean sellEnabled = true;
    private boolean sellAllEnabled = true;

    private SellAllDefinition sellAll = new SellAllDefinition();

    /** Key = ItemId (e.g. "Soil_Dirt"), Value = price rules. */
    private Map<String, SellPriceDefinition> prices = new HashMap<>();

    public SellConfig() {}

    // ---------- Convenience lookup ----------
    public SellPriceDefinition getPriceForItemId(String itemId) {
        if (itemId == null || itemId.isBlank() || prices.isEmpty()) return null;

        return prices.get(itemId);
    }

    // ---------- Getters ----------
    public boolean isEnabled() { return enabled; }
    public boolean isSellEnabled() { return sellEnabled; }
    public boolean isSellAllEnabled() { return sellAllEnabled; }
    public SellAllDefinition getSellAll() { return sellAll; }
    public Map<String, SellPriceDefinition> getPrices() { return prices; }

    // =====================================================================
    // Nested config blocks
    // =====================================================================


}

