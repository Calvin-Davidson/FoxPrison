package nl.trifox.foxprison.modules.mines.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.prefab.selection.mask.BlockPattern;

public class MineDefinition {

    public static final BuilderCodec<MineDefinition> CODEC =
            BuilderCodec.builder(MineDefinition.class, MineDefinition::new)
                    .append(new KeyedCodec<>("Id", Codec.STRING),
                            (m, v) -> m.id = v,
                            m -> m.id)
                    .add()
                    .append(new KeyedCodec<>("DisplayName", Codec.STRING),
                            (m, v) -> m.displayName = v,
                            m -> m.displayName)
                    .add()
                    .append(new KeyedCodec<>("World", Codec.STRING),
                            (m, v) -> m.world = v,
                            m -> m.world)
                    .add()
                    .append(new KeyedCodec<>("Spawn", Transform.CODEC),
                            (m, v) -> m.spawn = v,
                            m -> m.spawn)
                    .add()
                    .append(new KeyedCodec<>("Region", MineRegionDefinition.CODEC),
                            (m, v) -> m.region = v,
                            m -> m.region)
                    .add()
                    .append(new KeyedCodec<>("Blocks", BlockPattern.CODEC),
                            (m, v) -> m.blocks = v,
                            MineDefinition::getBlockPattern)
                    .add()
                    .append(new KeyedCodec<>("AutoReset", AutoResetDefinition.CODEC),
                            (m, v) -> m.autoReset = v,
                            MineDefinition::getAutoReset)
                    .add()
                    .append(new KeyedCodec<>("Requirements", MineRequirementsDefinition.CODEC),
                            MineDefinition::setRequirements,
                            MineDefinition::getRequirements)
                    .add()
                    .build();

    private String id = "a";
    private String displayName = "Mine A";
    private String world = "default";

    // Always non-null in saved config
    private Transform spawn = new Transform(0,0,0);
    private MineRegionDefinition region = new MineRegionDefinition();
    private AutoResetDefinition autoReset = new AutoResetDefinition();
    private BlockPattern blocks;
    private MineRequirementsDefinition requirements = new MineRequirementsDefinition();

    public MineDefinition() {
    }

    /**
     * Create a mine with 1 box region. Spawn optional:
     * - If spawn is null, we auto-place it at (centerX, maxY + 1, centerZ).
     */
    public static MineDefinition create(
            String id,
            String displayName,
            String world,
            Vector3i min,
            Vector3i max,
            Transform transform
    ) {
        // Normalize bounds so min <= max
        Vector3i nMin = new Vector3i(
                Math.min(min.getX(), max.getX()),
                Math.min(min.getY(), max.getY()),
                Math.min(min.getZ(), max.getZ())
        );
        Vector3i nMax = new Vector3i(
                Math.max(min.getX(), max.getX()),
                Math.max(min.getY(), max.getY()),
                Math.max(min.getZ(), max.getZ())
        );

        Transform spawn = (transform != null)
                ? transform
                : new Transform(
                (double) (nMin.getX() + nMax.getX()) / 2,
                nMax.getY() + 1,
                (double) (nMin.getZ() + nMax.getZ()) / 2
        );

        MineRegionDefinition reg = new MineRegionDefinition();
        reg.setBoxes(new BoxRegionDefinition[]{BoxRegionDefinition.create(nMin, nMax)});

        MineDefinition m = new MineDefinition();
        m.id = id;
        m.displayName = displayName;
        m.world = world;
        m.spawn = spawn;
        m.region = reg;

        return m;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getWorld() {
        return world;
    }

    public Transform getSpawn() {
        return spawn;
    }

    public MineRegionDefinition getRegion() {
        return region;
    }

    public BlockPattern getBlockPattern() {
        return blocks == null ? BlockPattern.parse("100%rock_stone") : blocks;
    }

    public AutoResetDefinition getAutoReset() {
        return this.autoReset == null ? new AutoResetDefinition() : autoReset;
    }

    public void setBlockPattern(BlockPattern blocks) {
        this.blocks = blocks;
    }

    public MineRequirementsDefinition getRequirements() {
        return requirements;
    }

    public void setRequirements(MineRequirementsDefinition requirements) {
        this.requirements = requirements;
    }
}
