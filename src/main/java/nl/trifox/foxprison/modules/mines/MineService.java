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

import nl.trifox.foxprison.api.interfaces.PlayerRankService;
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
    private final PlayerRankService playerRankService;
    private final MineResetter mineResetter;

    private final ConcurrentHashMap<String, MineRuntimeState> mineStates = new ConcurrentHashMap<>();


    public MineService(Config<CoreConfig> core, Config<MinesConfig> mines, PlayerRankService playerRankService) {
        this.core = core;
        this.mines = mines;
        this.playerRankService = playerRankService;
        mineResetter = new MineResetter();
    }

    public void mine(PlayerRef player, String mineIdOrNull) {
        String requested = (mineIdOrNull == null ? "" : mineIdOrNull.trim());

        // async fetch player rank id
        playerRankService.getRankID(player.getUuid()).thenAccept(playerRankId -> {

            MineDefinition target;

            if (!requested.isBlank()) {
                var mineOpt = findMine(requested);
                if (mineOpt.isEmpty()) {
                    player.sendMessage(Message.raw("Unknown mine: " + requested));
                    return;
                }

                var requestedMine = mineOpt.get();
                if (isRankAllowed(requestedMine, playerRankId)) {
                    target = requestedMine;
                } else {
                    target = findMaxMineForPlayerRank(playerRankId);
                    if (target == null) {
                        player.sendMessage(Message.raw("You can't access any mines yet."));
                        return;
                    }
                    player.sendMessage(Message.raw("You can't access that mine yet. Sending you to: " + target.getDisplayName()));
                }
            } else {
                target = findMaxMineForPlayerRank(playerRankId);
                if (target == null) {
                    // fallback: default mine id / first mine
                    target = findMine(core.get().getDefaultMineId()).orElseGet(() -> {
                        MineDefinition[] arr = mines.get().getMines();
                        return (arr != null && arr.length > 0) ? arr[0] : null;
                    });
                    if (target == null) {
                        player.sendMessage(Message.raw("No mines are configured."));
                        return;
                    }
                }
            }

            teleportToMine(player, target);
            player.sendMessage(Message.raw("Teleporting to " + target.getDisplayName()
                    + " @ " + target.getWorld() + " (" + target.getSpawn().getPosition().getX() + ", " + target.getSpawn().getPosition().getY() + ", " + target.getSpawn().getPosition().getZ() + ")"));

        }).exceptionally(err -> {
            player.sendMessage(Message.raw("Could not determine your rank right now."));
            return null;
        });
    }


    public void teleportToMine(PlayerRef player, MineDefinition mine) {
        var spawn = mine.getSpawn();
        var world = Universe.get().getWorld(mine.getWorld());
        var teleport = Teleport.createForPlayer(world, spawn.getPosition(), spawn.getRotation());

        Store<EntityStore> store = player.getReference().getStore();
        store.addComponent(player.getReference(), Teleport.getComponentType(), teleport);
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
        MineDefinition[] newArr = Arrays.copyOf(oldArr, oldArr.length + 1);
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

        return mineResetter.resetMine(mine, pattern, world);
    }

    public void startAutoResetLoop(TaskRegistry taskRegistry) {
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

        if (st.brokenBlocks.compareAndSet(0, 0)) return; // don't reset mines that have nothing to reset.

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


    public CompletableFuture<Boolean> setAutoReset(String id, Boolean aBoolean, Integer intervalSeconds, Integer blocksBroken, Integer minDelayBetweenResets) {
        var mineOpt = findMine(id);
        if (mineOpt.isEmpty()) return CompletableFuture.completedFuture(false);

        var mine = mineOpt.get();
        var def = new AutoResetDefinition();
        def.setEnabled(aBoolean);
        def.setMinSecondsBetweenResets(minDelayBetweenResets);
        def.setBlocksBrokenThreshold(blocksBroken);
        def.setIntervalSeconds(intervalSeconds);
        mine.setAutoReset(def);
        return mines.save().thenApply(_ -> true);
    }

    private boolean isRankAllowed(MineDefinition mine, String playerRankId) {
        if (mine == null) return false;

        MineRequirementsDefinition req = mine.getRequirements();
        if (req == null) return true;

        String[] allowed = req.getAllowedRanks();
        if (allowed.length == 0) return true;
        if (playerRankId == null || playerRankId.isBlank()) return false;

        for (String r : allowed) {
            if (r != null && r.equalsIgnoreCase(playerRankId)) {
                return true;
            }
        }
        return false;
    }
    
    private MineDefinition findMaxMineForPlayerRank(String playerRankId) {
        MineDefinition best = null;
        int bestTier = Integer.MIN_VALUE;

        MineDefinition[] all = mines.get().getMines();
        if (all == null) return null;

        for (MineDefinition m : all) {
            if (!isRankAllowed(m, playerRankId)) continue;

            int tier = mineTier(m);
            if (tier >= bestTier) {
                bestTier = tier;
                best = m;
            }
        }

        return best;
    }

    private int mineTier(MineDefinition mine) {
        MineRequirementsDefinition req = mine.getRequirements();
        if (req == null) return 0;

        return mine.getOrder();
    }
}
