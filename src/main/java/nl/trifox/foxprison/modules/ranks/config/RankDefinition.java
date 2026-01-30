package nl.trifox.foxprison.modules.ranks.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public final class RankDefinition {

    public static final BuilderCodec<RankDefinition> CODEC =
            BuilderCodec.builder(RankDefinition.class, RankDefinition::new)
                    .append(new KeyedCodec<>("Id", Codec.STRING),
                            (r, v, i) -> r.id = v,
                            (r, i) -> r.id)
                    .add()
                    .append(new KeyedCodec<>("DisplayName", Codec.STRING),
                            (r, v, i) -> r.displayName = v,
                            (r, i) -> r.displayName)
                    .add()
                    .append(new KeyedCodec<>("Costs", RankCostsDefinition.CODEC),
                            (r, v, i) -> r.costs = (v == null ? new RankCostsDefinition() : v),
                            (r, i) -> r.costs)
                    .add()
                    .build();

    private String id = "a";
    private String displayName = "A";

    private RankCostsDefinition costs = new RankCostsDefinition();

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }

    public RankCostsDefinition getCosts() {
        if (costs == null) costs = new RankCostsDefinition();
        return costs;
    }

}
