package nl.trifox.foxprison.config;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.math.vector.Vector3i;

public class BoxRegionDefinition {

    public static final BuilderCodec<BoxRegionDefinition> CODEC =
            BuilderCodec.builder(BoxRegionDefinition.class, BoxRegionDefinition::new)
                    .append(new KeyedCodec<>("Min", Vector3i.CODEC),
                            (b, v) -> b.min = v,
                            b -> b.min)
                    .add()
                    .append(new KeyedCodec<>("Max", Vector3i.CODEC),
                            (b, v) -> b.max = v,
                            b -> b.max)
                    .add()
                    .build();

    private Vector3i min = new Vector3i(0, 0, 0);
    private Vector3i max = new Vector3i(0, 0, 0);

    public BoxRegionDefinition() {}

    public static BoxRegionDefinition create(Vector3i min, Vector3i max) {
        BoxRegionDefinition b = new BoxRegionDefinition();
        b.min = min;
        b.max = max;
        return b;
    }

    public Vector3i getMin() { return min; }
    public Vector3i getMax() { return max; }
}
