package nl.trifox.foxprison.modules.mines.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.math.vector.Vector3d;

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

    public Boolean contains(int x, int y, int z) {
        for (BoxRegionDefinition box : boxes) {
            if (box.contains(x, y, z)) return true;
        }
        return false;
    }

    public Boolean contains(Vector3d position) {
        return contains(position.toVector3i().getX(), position.toVector3i().getY(), position.toVector3i().getZ());
    }
}
