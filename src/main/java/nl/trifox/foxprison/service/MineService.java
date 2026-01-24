package nl.trifox.foxprison.service;

import com.hypixel.hytale.builtin.buildertools.BuilderToolsPlugin;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.prefab.selection.mask.BlockPattern;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.Config;

import nl.trifox.foxprison.FoxPrisonPlugin;
import nl.trifox.foxprison.config.*;
import nl.trifox.foxprison.data.PlayerDataStore;
import nl.trifox.foxprison.data.PlayerPrisonData;
import nl.trifox.foxprison.economy.Economy;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class MineService {

    private final FoxPrisonPlugin plugin;
    private final PlayerDataStore store;
    private final Economy economy;

    private final Config<CoreConfig> core;
    private final Config<MinesConfig> mines;

    public MineService(
            FoxPrisonPlugin plugin,
            PlayerDataStore store,
            Economy economy,
            Config<CoreConfig> core,
            Config<EconomyConfig> econCfg,
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
                var min = box.getMin();
                var max = box.getMax();

                // Normalize in case min/max are swapped
                final int minX = Math.min(min.getX(), max.getX());
                final int minY = Math.min(min.getY(), max.getY());
                final int minZ = Math.min(min.getZ(), max.getZ());

                final int maxX = Math.max(min.getX(), max.getX());
                final int maxY = Math.max(min.getY(), max.getY());
                final int maxZ = Math.max(min.getZ(), max.getZ());

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
}
