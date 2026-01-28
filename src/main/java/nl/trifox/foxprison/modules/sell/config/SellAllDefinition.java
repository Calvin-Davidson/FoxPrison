package nl.trifox.foxprison.modules.sell.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public final class SellAllDefinition {

    public static final BuilderCodec<SellAllDefinition> CODEC =
            BuilderCodec.builder(SellAllDefinition.class, SellAllDefinition::new)

                    .append(new KeyedCodec<>("IncludeHotbar", Codec.BOOLEAN),
                            (cfg, v) -> cfg.includeHotbar = v,
                            cfg -> cfg.includeHotbar)
                    .add()

                    .append(new KeyedCodec<>("IncludeMainInventory", Codec.BOOLEAN),
                            (cfg, v) -> cfg.includeMainInventory = v,
                            cfg -> cfg.includeMainInventory)
                    .add()

                    .append(new KeyedCodec<>("IncludeEquipment", Codec.BOOLEAN),
                            (cfg, v) -> cfg.includeEquipment = v,
                            cfg -> cfg.includeEquipment)
                    .add()

                    .append(new KeyedCodec<>("IncludeOffhand", Codec.BOOLEAN),
                            (cfg, v) -> cfg.includeOffhand = v,
                            cfg -> cfg.includeOffhand)
                    .add()

                    .append(new KeyedCodec<>("SkipUnsellable", Codec.BOOLEAN),
                            (cfg, v) -> cfg.skipUnsellable = v,
                            cfg -> cfg.skipUnsellable)
                    .add()

                    .build();

    private boolean includeHotbar = true;
    private boolean includeMainInventory = true;
    private boolean includeEquipment = false;
    private boolean includeOffhand = true;

    /** If true, /sellall ignores items that have no price entry (instead of erroring). */
    private boolean skipUnsellable = true;

    public SellAllDefinition() {}

    public boolean isIncludeHotbar() { return includeHotbar; }
    public boolean isIncludeMainInventory() { return includeMainInventory; }
    public boolean isIncludeEquipment() { return includeEquipment; }
    public boolean isIncludeOffhand() { return includeOffhand; }
    public boolean isSkipUnsellable() { return skipUnsellable; }
}
