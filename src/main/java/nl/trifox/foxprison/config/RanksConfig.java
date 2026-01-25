package nl.trifox.foxprison.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public final class RanksConfig {
    // Store as array for codec support
    private RankDefinition[] ranks = new RankDefinition[0];

    public static final Codec<RankDefinition[]> RANK_ARRAY_CODEC =
            new ArrayCodec<>(RankDefinition.CODEC, RankDefinition[]::new);

    public static final BuilderCodec<RanksConfig> CODEC =
            BuilderCodec.builder(RanksConfig.class, RanksConfig::new)
                    .append(new KeyedCodec<>("Ranks", RANK_ARRAY_CODEC),
                            (c, v) -> c.ranks = v,
                            c -> c.ranks)
                    .add()
                    .build();

    public RankDefinition[] getRanks() { return ranks; }

    public Optional<RankDefinition> getRank(String rankID) {
        return Arrays.stream(ranks).filter(x -> x.getId().equalsIgnoreCase(rankID)).findFirst();
    }

}