package nl.trifox.foxprison.service;

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
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.Config;

import nl.trifox.foxprison.FoxPrisonPlugin;
import nl.trifox.foxprison.config.*;
import nl.trifox.foxprison.config.mines.BoxRegionDefinition;
import nl.trifox.foxprison.config.mines.MineDefinition;
import nl.trifox.foxprison.config.mines.MineRegionDefinition;
import nl.trifox.foxprison.config.mines.MinesConfig;
import nl.trifox.foxprison.data.MineRuntimeState;
import nl.trifox.foxprison.data.player.PlayerDataStore;
import nl.trifox.foxprison.economy.Economy;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MineService {

    private final FoxPrisonPlugin plugin;
    private final PlayerDataStore store;
    private final Economy economy;

    private final Config<CoreConfig> core;
    private final Config<MinesConfig> mines;

    private final ConcurrentHashMap<String, MineRuntimeState> mineStates = new ConcurrentHashMap<>();


    public MineService(
            FoxPrisonPlugin plugin,
            PlayerDataStore store,
            Economy economy,
            Config<CoreConfig> core,
            Config<RanksConfig> ranks,
            Config<MinesConfig> mines
    ) {
        this.plugin = plugin;
        this.store = store;
        this.economy = economy;
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
        return Arrays.stream(mines.get().getMines()).filter(x -> x.getId().equalsIgnoreCase(mineId)).findFirst();
    }

    public CompletableFuture<Boolean> SetSpawnableBlockPattern(String mineId, BlockPattern pattern) {
        mineId = mineId.trim().toLowerCase();

        if (mineId.isBlank()) return CompletableFuture.completedFuture(false);

        MineDefinition mine = findMine(mineId).orElse(null);
        if (mine == null) return CompletableFuture.completedFuture(false);

        mine.setBlockPattern(pattern);
        return mines.save().thenApply(_ -> true);
    }

    public CompletableFuture<Boolean> resetMine(String mineId, Random random) {
        mineId = mineId.trim().toLowerCase();

        if (mineId.isBlank()) return CompletableFuture.completedFuture(false);

        MineDefinition mine = findMine(mineId).orElse(null);
        if (mine == null) return CompletableFuture.completedFuture(false);

        BlockPattern pattern = mine.getBlockPattern();
        var world = Universe.get().getWorld(mine.getWorld());

        if (world == null) {
            // this mines world does not exist
            return CompletableFuture.completedFuture(false);
        }

        for (BoxRegionDefinition box : mine.getRegion().getBoxes()) {
            world.execute(() -> {
                var min = box.getNormalizedMin();
                var max = box.getNormalizedMax();

                final int minX = min.getX();
                final int minY = min.getY();
                final int minZ = min.getZ();

                final int maxX = max.getX();
                final int maxY = max.getY();
                final int maxZ = max.getZ();

                for (int x = minX; x <= maxX; x++) {
                    for (int y = minY; y <= maxY; y++) {
                        for (int z = minZ; z <= maxZ; z++) {
                            var blockKey = Objects
                                    .requireNonNull(pattern.nextBlockTypeKey(random))
                                    .blockTypeKey();

                            world.setBlock(x, y, z, blockKey);
                        }
                    }
                }
            });
        }

        return CompletableFuture.completedFuture(true);
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

        resetMine(mineId, new Random())
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


    public void startAutoResetLoop(TaskRegistry taskRegistry) {
        // Runs off-thread; do NOT edit the world directly here.
        @SuppressWarnings("unchecked")
        ScheduledFuture<Void> future = (ScheduledFuture<Void>) HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(
                this::tickIntervalResets,
                1, 1, TimeUnit.SECONDS
        );

        // Important: ensures it’s cancelled automatically when plugin unloads. :contentReference[oaicite:3]{index=3}
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

    public CompletableFuture<Boolean> setAutoReset(String id, Boolean aBoolean, Integer integer, Integer integer1, Integer integer2) {

    }
}
