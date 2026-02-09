package nl.trifox.foxprison.modules.sell.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import org.bson.BsonDocument;

public final class AutoSellDefinition {

    public static final BuilderCodec<AutoSellDefinition> CODEC =
            BuilderCodec.builder(AutoSellDefinition.class, AutoSellDefinition::new)
                    .append(new KeyedCodec<>("IsAutoSellEnabled", Codec.BOOLEAN),
                            (cfg, v) -> cfg.isAutoSellEnabled = v,
                            cfg -> cfg.isAutoSellEnabled)
                    .add()
                    .build();

    private boolean isAutoSellEnabled = true;

    public boolean isAutoSellEnabled() {
        return isAutoSellEnabled;
    }

    public void setAutoSellEnabled(boolean newState) {
        this.isAutoSellEnabled = newState;
    }
}