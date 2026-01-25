package nl.trifox.foxprison.config.mines;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;

public class MinesConfig {

    public static final Codec<MineDefinition[]> MINE_ARRAY_CODEC =
            new ArrayCodec<>(MineDefinition.CODEC, MineDefinition[]::new);

    public static final BuilderCodec<MinesConfig> CODEC =
            BuilderCodec.builder(MinesConfig.class, MinesConfig::new)
                    .append(new KeyedCodec<>("Mines", MINE_ARRAY_CODEC),
                            (c, v) -> c.mines = v,
                            c -> c.mines)
                    .add()
                    .build();

    private MineDefinition[] mines = new MineDefinition[0];

    public MineDefinition[] getMines() { return mines; }
    public void setMines(MineDefinition[] mines) { this.mines = mines; }
}
