package nl.trifox.foxprison.modules.ranks.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class RankDefinition {

    public static final BuilderCodec<RankDefinition> CODEC =
            BuilderCodec.builder(RankDefinition.class, RankDefinition::new)
                    .append(new KeyedCodec<String>("Id", Codec.STRING),
                            (r, v, i) -> r.id = v,
                            (r, i) -> r.id)
                    .add()
                    .append(new KeyedCodec<String>("DisplayName", Codec.STRING),
                            (r, v, i) -> r.displayName = v,
                            (r, i) -> r.displayName)
                    .add()
                    .append(new KeyedCodec<Double>("Cost", Codec.DOUBLE),
                            (r, v, i) -> r.cost = v,
                            (r, i) -> r.cost)
                    .add()
                    .build();

    private String id = "a";
    private String displayName = "A";
    private double cost = 0.0;

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public double getCost() { return cost; }
}
