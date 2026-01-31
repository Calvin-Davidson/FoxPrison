package nl.trifox.foxprison.modules.mines.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.prefab.selection.mask.BlockPattern;

public class MineProtectionDefinition {

    public static final BuilderCodec<MineProtectionDefinition> CODEC =
            BuilderCodec.builder(MineProtectionDefinition.class, MineProtectionDefinition::new)
                    .append(new KeyedCodec<>("DisableBreakingBlocksOutsideMine", Codec.BOOLEAN),
                            (c, v, i) -> c.preventBlockBreakingOutsideMineInWorld = v,
                            (c, i) -> c.preventBlockBreakingOutsideMineInWorld)
                    .add()
                    .build();

    private boolean preventBlockBreakingOutsideMineInWorld;


    public boolean isPreventBlockBreakingOutsideMineInWorld() {
        return preventBlockBreakingOutsideMineInWorld;
    }
}
