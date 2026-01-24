package nl.trifox.foxprison.config;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.math.vector.Vector3i;

import java.util.Vector;

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

    public Vector3i getNormalizedMin() {
        final int minX = Math.min(min.getX(), max.getX());
        final int minY = Math.min(min.getY(), max.getY());
        final int minZ = Math.min(min.getZ(), max.getZ());
        return new Vector3i(minX, minY, minZ);
    }

    public Vector3i getNormalizedMax() {
        final var maxX = Math.max(min.getX(), max.getX());
        final var maxY = Math.max(min.getY(), max.getY());
        final var maxZ = Math.max(min.getZ(), max.getZ());
        return new Vector3i(maxX, maxY, maxZ);
    }

    public boolean contains(int x, int y, int z) {
        final var nMin = getNormalizedMin();
        final var nMax = getNormalizedMax();

        return x >= nMin.getX() && x <= nMax.getX()
                && y >= nMin.getY() && y <= nMax.getY()
                && z >= nMin.getZ() && z <= nMax.getZ();
    }
}
