package nl.trifox.foxprison.modules.mines;

import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.prefab.selection.mask.BlockPattern;
import com.hypixel.hytale.server.core.universe.world.SetBlockSettings;
import com.hypixel.hytale.server.core.universe.world.World;
import nl.trifox.foxprison.modules.mines.config.BoxRegionDefinition;
import nl.trifox.foxprison.modules.mines.config.MineDefinition;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class MineResetter {

    public CompletableFuture<Boolean> resetMine(MineDefinition mine, BlockPattern pattern, World world) {
        int settings = SetBlockSettings.NO_NOTIFY +
                SetBlockSettings.NO_BREAK_FILLER +
                SetBlockSettings.NO_SEND_PARTICLES +
                SetBlockSettings.NO_DROP_ITEMS +
                SetBlockSettings.NO_SEND_AUDIO +
                SetBlockSettings.NO_UPDATE_HEIGHTMAP;


        BoxRegionDefinition[] boxes = mine.getRegion().getBoxes();
        if (boxes == null || boxes.length == 0) return CompletableFuture.completedFuture(false);

        Random jobRandom = new Random(System.nanoTime());

        CompletableFuture<Boolean> result = new CompletableFuture<>();
        MineResetJob job = new MineResetJob(world, boxes, pattern, jobRandom, settings, result);
        world.execute(job::runBatch);

        return result;
    }

    private static final class MineResetJob {
        // Per-tick time budget. Start LOW (1–3ms) for massive regions.
        // Worlds tick at ~30 TPS by default. :contentReference[oaicite:2]{index=2}
        private static final long BUDGET_NANOS = TimeUnit.MILLISECONDS.toNanos(2);

        private final World world;
        private final BoxRegionDefinition[] boxes;
        private final BlockPattern pattern;
        private final Random random;
        private final int setBlockSettings;
        private final CompletableFuture<Boolean> result;

        private int boxIndex = 0;

        private int minX, minY, minZ;
        private int maxX, maxY, maxZ;
        private int x, y, z;
        private boolean boxInitialized = false;

        private MineResetJob(World world,
                             BoxRegionDefinition[] boxes,
                             BlockPattern pattern,
                             Random random,
                             int settings,
                             CompletableFuture<Boolean> result) {
            this.world = world;
            this.boxes = boxes;
            this.pattern = pattern;
            this.random = random;
            this.setBlockSettings = settings;
            this.result = result;
        }

        void runBatch() {
            if (result.isDone()) return;
            if (!world.isAlive()) { // world.execute can reject tasks during shutdown :contentReference[oaicite:3]{index=3}
                result.complete(false);
                return;
            }

            try {
                final long start = System.nanoTime();

                while (System.nanoTime() - start < BUDGET_NANOS) {
                    if (boxIndex >= boxes.length) {
                        result.complete(true);
                        return;
                    }

                    if (!boxInitialized) initBox(boxes[boxIndex]);

                    var next = pattern.nextBlockTypeKey(random);
                    if (next == null) {
                        result.complete(false);
                        return;
                    }

                    world.setBlock(x, y, z, next.blockTypeKey(), setBlockSettings);
                    advanceCursor();
                }

                // IMPORTANT PART:
                // Don't immediately world.execute(this::runBatch) from inside the world thread,
                // because large queues can get drained in the same tick and "stick" the world.
                //
                // Instead: schedule a tiny delay off-thread, then hop back to world thread.
                long tickMs = Math.max(1L, world.getTickStepNanos() / 1_000_000L);

                HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
                    if (result.isDone() || !world.isAlive()) return;
                    world.execute(this::runBatch);
                }, tickMs, TimeUnit.MILLISECONDS);

            } catch (Throwable t) {
                result.completeExceptionally(t);
            }
        }

        private void initBox(BoxRegionDefinition box) {
            var min = box.getNormalizedMin();
            var max = box.getNormalizedMax();

            minX = min.getX(); minY = min.getY(); minZ = min.getZ();
            maxX = max.getX(); maxY = max.getY(); maxZ = max.getZ();

            x = minX; y = minY; z = minZ;
            boxInitialized = true;
        }

        private void advanceCursor() {
            z++;
            if (z <= maxZ) return;

            z = minZ;
            y++;
            if (y <= maxY) return;

            y = minY;
            x++;
            if (x <= maxX) return;

            boxIndex++;
            boxInitialized = false;
        }
    }
}
