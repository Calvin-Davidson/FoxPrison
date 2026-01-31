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

    private final ConcurrentHashMap<UUID, Object> locks = new ConcurrentHashMap<>();

    private final ExecutorService io =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "FoxPrison-IO-Ranks");
                t.setDaemon(true);
                return t;
            });

    public JsonPlayerRankRepository(FoxPrisonPlugin plugin, File folder) {
        this.plugin = plugin;
        this.folder = folder;
    }

    private Object lock(UUID id) {
        return locks.computeIfAbsent(id, k -> new Object());
    }

    private File file(UUID id) {
        return new File(folder, id.toString() + ".json");
    }

    private boolean writeSync(UUID playerId, PlayerRankData data) {
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

    @Override
    public CompletableFuture<PlayerRankData> getOrCreate(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (lock(playerId)) {
                File f = file(playerId);
                if (f.exists()) {
                    try (FileReader reader = new FileReader(f)) {
                        PlayerRankData loaded = gson.fromJson(reader, PlayerRankData.class);
                        if (loaded == null) loaded = new PlayerRankData();
                        return loaded;
                    } catch (IOException e) {
                        plugin.getLogger().atSevere().log("Could not read rank data for " + playerId, e);
                        PlayerRankData fallback = new PlayerRankData();
                        return fallback;
                    }
                }

                // Create new + persist synchronously (NO save().join() here)
                PlayerRankData created = new PlayerRankData();
                writeSync(playerId, created);
                return created;
            }
        }, io);
    }

    @Override
    public CompletableFuture<Boolean> save(UUID playerId, PlayerRankData data) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (lock(playerId)) {
                return writeSync(playerId, data);
            }
        }, io);
    }

    @Override
    public CompletableFuture<PlayerRankData> update(UUID playerId, UnaryOperator<PlayerRankData> mutator) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (lock(playerId)) {
                // Load (cache first, then disk)
                PlayerRankData current = null;

                File f = file(playerId);
                if (f.exists()) {
                    try (FileReader reader = new FileReader(f)) {
                        current = gson.fromJson(reader, PlayerRankData.class);
                    } catch (IOException e) {
                        plugin.getLogger().atSevere().log("Could not read rank data for " + playerId, e);
                    }
                }
                if (current == null) current = new PlayerRankData();

                // Mutate safely
                PlayerRankData mutated;
                try {
                    mutated = mutator.apply(current);
                } catch (Exception ex) {
                    plugin.getLogger().atSevere().log("Rank mutator failed for " + playerId, ex);
                    mutated = current;
                }
                if (mutated == null) mutated = current;

                writeSync(playerId, mutated);

                return mutated;
            }
        }, io);
    }

    @Override
    public void close() {
        io.shutdown();
    }
}
