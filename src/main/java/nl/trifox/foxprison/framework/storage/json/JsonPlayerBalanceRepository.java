package nl.trifox.foxprison.framework.storage.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import nl.trifox.foxprison.FoxPrisonPlugin;
import nl.trifox.foxprison.framework.storage.repositories.PlayerBalanceRepository;
import nl.trifox.foxprison.modules.economy.data.PlayerBalanceData;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.UnaryOperator;

public final class JsonPlayerBalanceRepository implements PlayerBalanceRepository, AutoCloseable {

    private final FoxPrisonPlugin plugin;
    private final File folder;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private final ConcurrentHashMap<UUID, PlayerBalanceData> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Object> locks = new ConcurrentHashMap<>();

    private final ExecutorService io =
            Executors.newSingleThreadExecutor(r -> new Thread(r, "FoxPrison-IO-Balances"));

    public JsonPlayerBalanceRepository(FoxPrisonPlugin plugin, File folder) {
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
    public CompletableFuture<PlayerBalanceData> getOrCreate(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            // Fast path
            PlayerBalanceData cached = cache.get(playerId);
            if (cached != null) return cached;

            synchronized (lock(playerId)) {
                PlayerBalanceData again = cache.get(playerId);
                if (again != null) return again;

                File f = file(playerId);
                if (!f.exists()) {
                    PlayerBalanceData created = new PlayerBalanceData();
                    cache.put(playerId, created);
                    // Persist initial
                    save(playerId, created).join();
                    return created;
                }

                try (FileReader reader = new FileReader(f)) {
                    PlayerBalanceData loaded = gson.fromJson(reader, PlayerBalanceData.class);
                    if (loaded == null) loaded = new PlayerBalanceData();
                    cache.put(playerId, loaded);
                    return loaded;
                } catch (IOException e) {
                    plugin.getLogger().atSevere().log("Could not read balances for " + playerId, e);
                    PlayerBalanceData fallback = new PlayerBalanceData();
                    cache.put(playerId, fallback);
                    return fallback;
                }
            }
        }, io);
    }

    @Override
    public CompletableFuture<Boolean> save(UUID playerId, PlayerBalanceData data) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (lock(playerId)) {
                cache.put(playerId, data);

                try {
                    if (!folder.exists() && !folder.mkdirs()) {
                        plugin.getLogger().atSevere().log("Could not create balances folder");
                        return false;
                    }

                    try (FileWriter writer = new FileWriter(file(playerId))) {
                        gson.toJson(data, writer);
                    }
                    return true;
                } catch (IOException e) {
                    plugin.getLogger().atSevere().log("Could not save balances for " + playerId, e);
                    return false;
                }
            }
        }, io);
    }

    @Override
    public CompletableFuture<Boolean> delete(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (lock(playerId)) {
                // Remove cached copy
                cache.remove(playerId);

                // Delete file
                File f = file(playerId);
                if (!f.exists()) {
                    return true; // already gone
                }

                try {
                    return f.delete();
                } catch (SecurityException e) {
                    plugin.getLogger().atSevere().log("Could not delete balances for " + playerId, e);
                    return false;
                } finally {
                    // Optional: clean up lock map to avoid unbounded growth
                    locks.remove(playerId);
                }
            }
        }, io);
    }

    @Override
    public CompletableFuture<PlayerBalanceData> update(UUID playerId, UnaryOperator<PlayerBalanceData> mutator) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (lock(playerId)) {
                PlayerBalanceData current = cache.get(playerId);
                if (current == null) {
                    // Load on-demand inside lock
                    File f = file(playerId);
                    if (f.exists()) {
                        try (FileReader reader = new FileReader(f)) {
                            current = gson.fromJson(reader, PlayerBalanceData.class);
                        } catch (IOException e) {
                            plugin.getLogger().atSevere().log("Could not read balances for " + playerId, e);
                        }
                    }
                    if (current == null) current = new PlayerBalanceData();
                }

                PlayerBalanceData mutated = mutator.apply(current);
                if (mutated == null) mutated = current;

                cache.put(playerId, mutated);

                // Save
                try (FileWriter writer = new FileWriter(file(playerId))) {
                    gson.toJson(mutated, writer);
                } catch (IOException e) {
                    plugin.getLogger().atSevere().log("Could not save balances for " + playerId, e);
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
