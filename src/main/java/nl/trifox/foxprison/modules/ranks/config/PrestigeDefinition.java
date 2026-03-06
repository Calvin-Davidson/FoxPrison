package nl.trifox.foxprison.modules.ranks.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Represents a single prestige level configuration.
 * Includes:
 *  - prestige number
 *  - multiplier (applied to rewards/sell)
 *  - rankup multiplier (applied to rankup costs)
 *  - cost (currency cost to unlock prestige)
 */
public class PrestigeDefinition {

    private int prestige;
    private double multiplier = 0.0;
    private double rankupMultiplier = 0.0;
    private double cost = 0.0;
    private RankCostsDefinition costs = new RankCostsDefinition();


    public PrestigeDefinition() {}

    public PrestigeDefinition(int prestige, double multiplier, double rankupMultiplier, double cost) {
        this.prestige = prestige;
        this.multiplier = multiplier;
        this.rankupMultiplier = rankupMultiplier;
        this.cost = cost;
    }

    public static final BuilderCodec<PrestigeDefinition> CODEC =
            BuilderCodec.builder(PrestigeDefinition.class, PrestigeDefinition::new)
                    .append(new KeyedCodec<>("Prestige", Codec.INTEGER),
                            (r, v, i) -> r.prestige = v,
                            (r, i) -> r.prestige)
                    .add()
                    .append(new KeyedCodec<>("Multiplier", Codec.DOUBLE),
                            (r, v, i) -> r.multiplier = v,
                            (r, i) -> r.multiplier)
                    .add()
                    .append(new KeyedCodec<>("RankupMultiplier", Codec.DOUBLE),
                            (r, v, i) -> r.rankupMultiplier = v,
                            (r, i) -> r.rankupMultiplier)
                    .add()
                    .append(new KeyedCodec<>("Costs", RankCostsDefinition.CODEC),
                            (r, v, i) -> r.costs = (v == null ? new RankCostsDefinition() : v),
                            (r, i) -> r.costs)
                    .add()
                    .build();

    // --- Getters & setters ---

    public int getPrestige() {
        return prestige;
    }

    public void setPrestige(int prestige) {
        this.prestige = prestige;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(double multiplier) {
        this.multiplier = multiplier;
    }

    public double getRankupMultiplier() {
        return rankupMultiplier;
    }

    public void setRankupMultiplier(double rankupMultiplier) {
        this.rankupMultiplier = rankupMultiplier;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }
}