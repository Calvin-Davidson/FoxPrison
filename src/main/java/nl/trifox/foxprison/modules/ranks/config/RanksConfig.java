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
    private RankDefinition[] ranks;
    private PrestigeDefinition[] prestiges;

    public RanksConfig() {
        // Defaults when config is first created (or when key is absent depending on codec behavior)
        this.ranks = defaultRanksAZ();
    }

    public static final Codec<RankDefinition[]> RANK_ARRAY_CODEC =
            new ArrayCodec<>(RankDefinition.CODEC, RankDefinition[]::new);

    public static final Codec<PrestigeDefinition[]> PRESTIGE_ARRAY_CODEC =
            new ArrayCodec<>(PrestigeDefinition.CODEC, PrestigeDefinition[]::new);


    public static final BuilderCodec<RanksConfig> CODEC =
            BuilderCodec.builder(RanksConfig.class, RanksConfig::new)
                    .append(new KeyedCodec<>("Ranks", RANK_ARRAY_CODEC),
                            (c, v) -> {
                                // If the file has no "Ranks" key (null) or it's empty, fall back to defaults.
                                if (v == null || v.length == 0) {
                                    c.ranks = defaultRanksAZ();
                                } else {
                                    c.ranks = v;
                                }
                            },
                            c -> c.ranks)
                    .add()
                    .append(new KeyedCodec<>("Prestige", PRESTIGE_ARRAY_CODEC),
                            (c, v) -> {
                                if (v == null || v.length == 0) {
                                    c.prestiges = new PrestigeDefinition[0];
                                } else {
                                    c.prestiges = v;
                                }
                            },
                            c -> c.prestiges)
                    .add()
                    .build();

    public RankDefinition[] getRanks() {
        return ranks == null ? new RankDefinition[0] : ranks;
    }

    public void setRanks(RankDefinition[] ranks) {
        this.ranks = ranks;
    }

    public PrestigeDefinition[] getPrestiges() {
        return prestiges == null ? new PrestigeDefinition[0] : prestiges;
    }

    public Optional<RankDefinition> getRank(String rankID) {
        if (rankID == null) return Optional.empty();
        String id = rankID.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(getRanks())
                .filter(x -> x != null && x.getId().equalsIgnoreCase(id))
                .findFirst();
    }

    /**
     * Creates default ranks A-Z.
     * - Id: "A".."Z"
     * - DisplayName: "A".."Z"
     * - UnlockMineId: same as Id
     * - Cost: starts at 100, grows by 1.25x each rank (rounded to 2 decimals)
     */
    private static RankDefinition[] defaultRanksAZ() {
        RankDefinition[] arr = new RankDefinition[26];

        double baseCost = 100.0;
        double growth = 1.25;

        for (int i = 0; i < 26; i++) {
            char letter = (char) ('A' + i);
            String id = String.valueOf(letter);

            double cost = round2(baseCost * Math.pow(growth, i));
            arr[i] = new RankDefinition(id, id, cost);
        }

        return arr;
    }

    public double getPrestigeMultiplier(int prestigeLevel) {
        PrestigeDefinition[] arr = getPrestiges();
        if (arr.length == 0 || prestigeLevel <= 0) return 0.0;

        if (prestigeLevel > arr.length) prestigeLevel = arr.length;

        return arr[prestigeLevel - 1].getMultiplier();
    }

    /**
     * Returns the rankup multiplier for a given prestige level.
     */
    public double getPrestigeRankupMultiplier(int prestigeLevel) {
        PrestigeDefinition[] arr = getPrestiges();
        if (arr.length == 0 || prestigeLevel <= 0) return 0.0;

        if (prestigeLevel > arr.length) prestigeLevel = arr.length;

        return arr[prestigeLevel - 1].getRankupMultiplier();
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
