package nl.trifox.foxprison.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class CoreConfig {

    public static final BuilderCodec<CoreConfig> CODEC =
            BuilderCodec.builder(CoreConfig.class, CoreConfig::new)
                    .append(new KeyedCodec<Boolean>("Enabled", Codec.BOOLEAN),
                            (c, v, i) -> c.enabled = v,
                            (c, i) -> c.enabled)
                    .add()
                    .append(new KeyedCodec<String>("DefaultMineId", Codec.STRING),
                            (c, v, i) -> c.defaultMineId = v,
                            (c, i) -> c.defaultMineId)
                    .add()
                    .append(new KeyedCodec<Boolean>("Debug", Codec.BOOLEAN),
                            (c, v, i) -> c.debug = v,
                            (c, i) -> c.debug)
                    .add()
                    .build();

    private boolean enabled = true;
    private String defaultMineId = "a";
    private boolean debug = false;

    public boolean isEnabled() { return enabled; }
    public String getDefaultMineId() { return defaultMineId; }
    public boolean isDebug() { return debug; }
}
