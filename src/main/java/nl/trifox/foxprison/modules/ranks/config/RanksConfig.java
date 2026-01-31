package nl.trifox.foxprison.modules.ranks.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public final class RanksConfig {

    // Store as array for codec support
    private RankDefinition[] ranks = new RankDefinition[0];

    public static final Codec<RankDefinition[]> RANK_ARRAY_CODEC =
            new ArrayCodec<>(RankDefinition.CODEC, RankDefinition[]::new);

    public static final BuilderCodec<RanksConfig> CODEC =
            BuilderCodec.builder(RanksConfig.class, RanksConfig::new)
                    .append(new KeyedCodec<>("Ranks", RANK_ARRAY_CODEC),
                            (c, v) -> c.ranks = (v == null ? new RankDefinition[0] : v),
                            c -> c.ranks)
                    .add()
                    .build();

    public RankDefinition[] getRanks() {
        return ranks == null ? new RankDefinition[0] : ranks;
    }

    public Optional<RankDefinition> getRank(String rankID) {
        if (rankID == null) return Optional.empty();
        String id = rankID.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(getRanks())
                .filter(x -> x != null && x.getId().equalsIgnoreCase(id))
                .findFirst();
    }
}