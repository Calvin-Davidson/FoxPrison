package nl.trifox.foxprison.framework.storage.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import nl.trifox.foxprison.FoxPrisonPlugin;
import nl.trifox.foxprison.framework.storage.repositories.PlayerRankRepository;
import nl.trifox.foxprison.modules.ranks.data.PlayerRankData;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.UnaryOperator;

public final class JsonPlayerRankRepository implements PlayerRankRepository, AutoCloseable {

    private final FoxPrisonPlugin plugin;
    private final File folder;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private final ConcurrentHashMap<UUID, PlayerRankData> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Object> locks = new ConcurrentHashMap<>();

    private final ExecutorService io =
            Executors.newSingleThreadExecutor(r -> new Thread(r, "FoxPrison-IO-Ranks"));

    public JsonPlayerRankRepository(FoxPrisonPlugin plugin, File folder) {
        this.plugin = plugin;
        this.folder = folder;
    }

    private Object lock(UUID id) {
        return locks.computeIfAbsent(id, _ -> new Object());
    }

    private File file(UUID id) {
        return new File(folder, id.toString() + ".json");
    }

    @Override
    public CompletableFuture<PlayerRankData> getOrCreate(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            PlayerRankData cached = cache.get(playerId);
            if (cached != null) return cached;

            synchronized (lock(playerId)) {
                PlayerRankData again = cache.get(playerId);
                if (again != null) return again;

                File f = file(playerId);
                if (!f.exists()) {
                    PlayerRankData created = new PlayerRankData();
                    cache.put(playerId, created);
                    save(playerId, created).join();
                    return created;
                }

                try (FileReader reader = new FileReader(f)) {
                    PlayerRankData loaded = gson.fromJson(reader, PlayerRankData.class);
                    if (loaded == null) loaded = new PlayerRankData();
                    cache.put(playerId, loaded);
                    return loaded;
                } catch (IOException e) {
                    plugin.getLogger().atSevere().log("Could not read rank data for " + playerId, e);
                    PlayerRankData fallback = new PlayerRankData();
                    cache.put(playerId, fallback);
                    return fallback;
                }
            }
        }, io);
    }

    @Override
    public CompletableFuture<Boolean> save(UUID playerId, PlayerRankData data) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (lock(playerId)) {
                cache.put(playerId, data);
                try {
                    if (!folder.exists() && !folder.mkdirs()) {
                        plugin.getLogger().atSevere().log("Could not create ranks folder");
                        return false;
                    }
                    try (FileWriter writer = new FileWriter(file(playerId))) {
                        gson.toJson(data, writer);
                    }
                    return true;
                } catch (IOException e) {
                    plugin.getLogger().atSevere().log("Could not save rank data for " + playerId, e);
                    return false;
                }
            }
        }, io);
    }

    @Override
    public CompletableFuture<PlayerRankData> update(UUID playerId, UnaryOperator<PlayerRankData> mutator) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (lock(playerId)) {
                PlayerRankData current = cache.get(playerId);
                if (current == null) {
                    File f = file(playerId);
                    if (f.exists()) {
                        try (FileReader reader = new FileReader(f)) {
                            current = gson.fromJson(reader, PlayerRankData.class);
                        } catch (IOException e) {
                            plugin.getLogger().atSevere().log("Could not read rank data for " + playerId, e);
                        }
                    }
                    if (current == null) current = new PlayerRankData();
                }

                PlayerRankData mutated = mutator.apply(current);
                if (mutated == null) mutated = current;

                cache.put(playerId, mutated);

                try (FileWriter writer = new FileWriter(file(playerId))) {
                    gson.toJson(mutated, writer);
                } catch (IOException e) {
                    plugin.getLogger().atSevere().log("Could not save rank data for " + playerId, e);
                }

                return mutated;
            }
        }, io);
    }

    @Override
    public void close() {
        io.shutdown();
    }
}