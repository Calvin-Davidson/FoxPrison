package nl.trifox.foxprison.modules.mines.data;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class MineRuntimeState {
    public final AtomicInteger brokenBlocks = new AtomicInteger(0);
    public final AtomicBoolean resetInProgress = new AtomicBoolean(false);

    public volatile long lastResetAtMs = 0L;
    public volatile long nextIntervalResetAtMs = 0L;

}