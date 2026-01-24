package nl.trifox.foxprison.config;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.math.vector.Vector3i;

public final class AutoResetDefinition {

    public static final BuilderCodec<AutoResetDefinition> CODEC =
            BuilderCodec.builder(AutoResetDefinition.class, AutoResetDefinition::new)
                    .append(new KeyedCodec<>("Enabled", BuilderCodec.BOOLEAN),
                            (b, v) -> b.enabled = v,
                            b -> b.enabled)
                    .add()
                    .append(new KeyedCodec<>("IntervalSeconds", BuilderCodec.INTEGER),
                            (b, v) -> b.intervalSeconds = v,
                            b -> b.intervalSeconds)
                    .add()
                    .append(new KeyedCodec<>("BlocksBrokenThreshold", BuilderCodec.INTEGER),
                            (b, v) -> b.blocksBrokenThreshold = v,
                            b -> b.blocksBrokenThreshold)
                    .add()
                    .append(new KeyedCodec<>("MinSecondsBetweenResets", BuilderCodec.INTEGER),
                            (b, v) -> b.minSecondsBetweenResets = v,
                            b -> b.minSecondsBetweenResets)
                    .add()
                    .build();

    private boolean enabled = false;

    // If > 0: reset every N seconds
    private int intervalSeconds = 0;

    // If > 0: reset after N blocks broken inside the mine
    private int blocksBrokenThreshold = 0;

    private int minSecondsBetweenResets = 30;

    public boolean isEnabled() { return enabled; }
    public int getIntervalSeconds() { return intervalSeconds; }
    public int getBlocksBrokenThreshold() { return blocksBrokenThreshold; }
    public int getMinSecondsBetweenResets() { return minSecondsBetweenResets; }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setIntervalSeconds(int intervalSeconds) { this.intervalSeconds = Math.max(0, intervalSeconds); }
    public void setBlocksBrokenThreshold(int blocksBrokenThreshold) { this.blocksBrokenThreshold = Math.max(0, blocksBrokenThreshold); }
    public void setMinSecondsBetweenResets(int minSecondsBetweenResets) { this.minSecondsBetweenResets = Math.max(0, minSecondsBetweenResets); }
}
