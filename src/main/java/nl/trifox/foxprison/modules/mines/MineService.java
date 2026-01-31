package nl.trifox.foxprison.modules.mines;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.prefab.selection.mask.BlockPattern;
import com.hypixel.hytale.server.core.task.TaskRegistry;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.SetBlockSettings;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.Config;

import nl.trifox.foxprison.framework.config.CoreConfig;
import nl.trifox.foxprison.modules.mines.config.*;
import nl.trifox.foxprison.modules.mines.data.MineRuntimeState;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MineService {

    private final Config<CoreConfig> core;
    private final Config<MinesConfig> mines;

    private final ConcurrentHashMap<String, MineRuntimeState> mineStates = new ConcurrentHashMap<>();


    public MineService(Config<CoreConfig> core, Config<MinesConfig> mines) {
        this.core = core;
        this.mines = mines;
    }

    public void mine(PlayerRef player, String mineIdOrNull) {
        String mineId = (mineIdOrNull == null || mineIdOrNull.isBlank())
                ? core.get().getDefaultMineId()
                : mineIdOrNull;

        Optional<MineDefinition> mine = findMine(mineId);
        if (mine.isEmpty()) {
            player.sendMessage(Message.raw("Unknown mine: " + mineId));
            return;
        }

        MineDefinition def = mine.get();
        var spawn = mine.get().getSpawn();
        var world = Universe.get().getWorld(mine.get().getWorld());
        var teleport = Teleport.createForPlayer(world, spawn.getPosition(), spawn.getRotation());

        Store<EntityStore> store = player.getReference().getStore();
        store.addComponent(player.getReference(), Teleport.getComponentType(), teleport);

        player.sendMessage(Message.raw("Teleporting to " + def.getDisplayName()
                + " @ " + def.getWorld() + " (" + def.getSpawn().getPosition().getX() + ", " + def.getSpawn().getPosition().getY() + ", " + def.getSpawn().getPosition().getZ() + ")"));
    }


    public List<MineDefinition> getAllMines() {
        return Arrays.asList(mines.get().getMines());
    }


    private Optional<MineDefinition> findMine(String id) {
        return Arrays.stream(mines.get().getMines())
                .filter(m -> m.getId().equalsIgnoreCase(id))
                .findFirst();
    }


    public CompletableFuture<Boolean> createMine(
            String id,
            String displayName,
            String world,
            int minX, int minY, int minZ,
            int maxX, int maxY, int maxZ,
            Transform transform
    ) {
        id = id.trim().toLowerCase();
        if (id.isBlank()) return CompletableFuture.completedFuture(false);
        if (findMine(id).isPresent()) return CompletableFuture.completedFuture(false);

        Vector3i min = new Vector3i(minX, minY, minZ);
        Vector3i max = new Vector3i(maxX, maxY, maxZ);

        var spawnPos = new Vector3d(transform.getPosition().getX(), transform.getPosition().getY(), transform.getPosition().getZ());
        var spawnRot = new Vector3f(transform.getRotation().getX(), transform.getRotation().getY(), transform.getRotation().getZ());
        var spawnTransform = new Transform(spawnPos, spawnRot);

        MineDefinition mine = MineDefinition.create(id, displayName, world, min, max, spawnTransform);

        MinesConfig cfg = mines.get();
        MineDefinition[] oldArr = cfg.getMines();
        MineDefinition[] newArr = java.util.Arrays.copyOf(oldArr, oldArr.length + 1);
        newArr[newArr.length - 1] = mine;

        cfg.setMines(newArr);

        return mines.save().thenApply(_ -> true);
    }

    public CompletableFuture<Boolean> setSpawnPoint(
            String id,
            Transform transform
    ) {
        id = id.trim().toLowerCase();
        if (id.isBlank()) return CompletableFuture.completedFuture(false);
        var mine = findMine(id);
        if (mine.isEmpty()) return CompletableFuture.completedFuture(false);

        var spawnPos = new Vector3d(transform.getPosition().getX(), transform.getPosition().getY(), transform.getPosition().getZ());
        var spawnRot = new Vector3f(transform.getRotation().getX(), transform.getRotation().getY(), transform.getRotation().getZ());
        var spawnTransform = new Transform(spawnPos, spawnRot);

        mine.get().setSpawn(spawnTransform);
        return mines.save().thenApply(_ -> true);
    }


    public CompletableFuture<Boolean> regionAddBox(
            String mineId,
            int minX, int minY, int minZ,
            int maxX, int maxY, int maxZ
    ) {
        MineDefinition mine = findMine(mineId).orElse(null);
        if (mine == null) return CompletableFuture.completedFuture(false);

        // normalize
        Vector3i min = new Vector3i(
                Math.min(minX, maxX),
                Math.min(minY, maxY),
                Math.min(minZ, maxZ)
        );
        Vector3i max = new Vector3i(
                Math.max(minX, maxX),
                Math.max(minY, maxY),
                Math.max(minZ, maxZ)
        );

        MineRegionDefinition region = mine.getRegion();
        BoxRegionDefinition[] oldBoxes = region.getBoxes();
        BoxRegionDefinition[] newBoxes = Arrays.copyOf(oldBoxes, oldBoxes.length + 1);
        newBoxes[newBoxes.length - 1] = BoxRegionDefinition.create(min, max);
        region.setBoxes(newBoxes);

        return mines.save().thenApply(_ -> true);
    }

    public CompletableFuture<Boolean> regionClear(String mineId) {
        MineDefinition mine = findMine(mineId).orElse(null);
        if (mine == null) return CompletableFuture.completedFuture(false);

        mine.getRegion().setBoxes(new BoxRegionDefinition[0]);

        return mines.save().thenApply(_ -> true);
    }


    public CompletableFuture<Boolean> deleteMine(String id) {
        id = id.trim().toLowerCase();
        MinesConfig cfg = mines.get();
        MineDefinition[] oldArr = cfg.getMines();

        int idx = -1;
        for (int i = 0; i < oldArr.length; i++) {
            if (oldArr[i].getId().equalsIgnoreCase(id)) { idx = i; break; }
        }
        if (idx == -1) return CompletableFuture.completedFuture(false);

        MineDefinition[] newArr = new MineDefinition[oldArr.length - 1];
        int w = 0;
        for (int i = 0; i < oldArr.length; i++) {
            if (i == idx) continue;
            newArr[w++] = oldArr[i];
        }

        cfg.setMines(newArr);
        return mines.save().thenApply(_ -> true);
    }

    public MineRuntimeState getState(String mineId) {
        return mineStates.computeIfAbsent(mineId.toLowerCase(), k -> new MineRuntimeState());
    }

    public Optional<MineDefinition> getMine(String mineId) {
        return findMine(mineId);
    }

    public CompletableFuture<Boolean> SetSpawnableBlockPattern(String mineId, BlockPattern pattern) {
        mineId = mineId.trim().toLowerCase();

        if (mineId.isBlank()) return CompletableFuture.completedFuture(false);

        MineDefinition mine = findMine(mineId).orElse(null);
        if (mine == null) return CompletableFuture.completedFuture(false);

        mine.setBlockPattern(pattern);
        return mines.save().thenApply(_ -> true);
    }

    public CompletableFuture<Boolean> resetMine(String mineId) {
        mineId = mineId.trim().toLowerCase(Locale.ROOT);
        if (mineId.isBlank()) return CompletableFuture.completedFuture(false);

        MineDefinition mine = findMine(mineId).orElse(null);
        if (mine == null) return CompletableFuture.completedFuture(false);

        BlockPattern pattern = mine.getBlockPattern();
        if (pattern == null || pattern.isEmpty()) return CompletableFuture.completedFuture(false);

        World world = Universe.get().getWorld(mine.getWorld());
        if (world == null) return CompletableFuture.completedFuture(false);

        // Settings object (reuse it; avoid per-call allocations)
        int settings = SetBlockSettings.NO_NOTIFY +
                SetBlockSettings.NO_BREAK_FILLER +
                SetBlockSettings.NO_SEND_PARTICLES +
                SetBlockSettings.NO_DROP_ITEMS +
                SetBlockSettings.NO_SEND_AUDIO +
                SetBlockSettings.NO_UPDATE_HEIGHTMAP;


        CompletableFuture<Boolean> result = new CompletableFuture<>();

        BoxRegionDefinition[] boxes = mine.getRegion().getBoxes();
        if (boxes == null || boxes.length == 0) return CompletableFuture.completedFuture(false);

        // Dedicated RNG for the job (stable + no shared contention)
        Random jobRandom = new Random(System.nanoTime());

        MineResetJob job = new MineResetJob(world, boxes, pattern, jobRandom, settings, result);

        // Start on the world thread
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
                // Instead: schedule a tiny delay off-thread, then hop back to world thread. :contentReference[oaicite:4]{index=4}
                long tickMs = Math.max(1L, world.getTickStepNanos() / 1_000_000L); // ~33ms at 30 TPS :contentReference[oaicite:5]{index=5}

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


    public void startAutoResetLoop(TaskRegistry taskRegistry) {
        // Runs off-thread; do NOT edit the world directly here.
        @SuppressWarnings("unchecked")
        ScheduledFuture<Void> future = (ScheduledFuture<Void>) HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(
                this::tickIntervalResets,
                1, 1, TimeUnit.SECONDS
        );

        taskRegistry.registerTask(future);
    }

    private void tickIntervalResets() {
        long now = System.currentTimeMillis();

        for (var mine : getAllMines()) {
            AutoResetDefinition ar = mine.getAutoReset();
            if (ar == null || !ar.isEnabled()) continue;

            int interval = ar.getIntervalSeconds();
            if (interval <= 0) continue;

            MineRuntimeState st = getState(mine.getId());

            if (st.nextIntervalResetAtMs == 0L) {
                st.nextIntervalResetAtMs = now + interval * 1000L;
            }

            if (now >= st.nextIntervalResetAtMs) {
                triggerResetIfAllowed(mine.getId(), now, "interval");
            }
        }
    }

    public void triggerResetIfAllowed(String mineId, long nowMs, String reason) {
        var mineOpt = findMine(mineId);
        if (mineOpt.isEmpty()) return;

        var mine = mineOpt.get();
        var ar = mine.getAutoReset();
        var st = getState(mineId);

        long minGapMs = (ar != null ? ar.getMinSecondsBetweenResets() : 5) * 1000L;
        if (st.lastResetAtMs != 0 && (nowMs - st.lastResetAtMs) < minGapMs) return;

        if (!st.resetInProgress.compareAndSet(false, true)) return;

        resetMine(mineId)
                .whenComplete((ok, err) -> {
                    st.resetInProgress.set(false);

                    if (err != null) {
                        return;
                    }

                    st.brokenBlocks.set(0);
                    st.lastResetAtMs = System.currentTimeMillis();

                    int interval = (ar != null ? ar.getIntervalSeconds() : 0);
                    st.nextIntervalResetAtMs = interval > 0
                            ? st.lastResetAtMs + interval * 1000L
                            : 0L;
                });
    }


    public CompletableFuture<Boolean> setAutoReset(String id, Boolean aBoolean, Integer integer, Integer integer1, Integer integer2) {
        return CompletableFuture.completedFuture(true);
    }
}
