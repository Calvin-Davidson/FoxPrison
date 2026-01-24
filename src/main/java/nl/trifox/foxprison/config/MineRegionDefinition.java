package nl.trifox.foxprison.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;

public class MineRegionDefinition {

    public static final Codec<BoxRegionDefinition[]> BOX_ARRAY_CODEC =
            new ArrayCodec<>(BoxRegionDefinition.CODEC, BoxRegionDefinition[]::new);

    public static final BuilderCodec<MineRegionDefinition> CODEC =
            BuilderCodec.builder(MineRegionDefinition.class, MineRegionDefinition::new)
                    .append(new KeyedCodec<>("Boxes", BOX_ARRAY_CODEC),
                            (r, v) -> r.boxes = v,
                            r -> r.boxes)
                    .add()
                    .build();

    private BoxRegionDefinition[] boxes = new BoxRegionDefinition[0];

    public BoxRegionDefinition[] getBoxes() { return boxes; }
    public void setBoxes(BoxRegionDefinition[] boxes) { this.boxes = boxes; }
}
