package nl.trifox.foxprison.modules.mines.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;


/**
 * Requirements needed for a player wanting to enter this mine
 */
public class MineRequirementsDefinition {

    public static final BuilderCodec<MineRequirementsDefinition> CODEC =
            BuilderCodec.builder(MineRequirementsDefinition.class, MineRequirementsDefinition::new)
                    .append(new KeyedCodec<>("Ranks", Codec.STRING_ARRAY),
                            (r, v) -> r.allowedRanks = v,
                            r -> r.allowedRanks)
                    .add()
                    .build();


    private String[] allowedRanks;

    public void setRankID(String[] ranks) {
        this.allowedRanks = ranks;
    }

    public String[] getAllowedRanks() {
        return allowedRanks == null ? new String[0] : allowedRanks;
    }

    public void setAllowedRanks(String[] allowedRanks) {
        this.allowedRanks = allowedRanks;
    }
}
