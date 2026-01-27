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
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "FoxPrison-IO-Balances");
                t.setDaemon(true);
                return t;
            });

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

    private boolean writeSync(UUID playerId, PlayerBalanceData data) {
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

    @Override
    public CompletableFuture<PlayerBalanceData> getOrCreate(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            PlayerBalanceData cached = cache.get(playerId);
            if (cached != null) return cached;

            synchronized (lock(playerId)) {
                PlayerBalanceData again = cache.get(playerId);
                if (again != null) return again;

                File f = file(playerId);
                if (f.exists()) {
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

                // Create new + persist synchronously (NO save().join() here)
                PlayerBalanceData created = new PlayerBalanceData();
                cache.put(playerId, created);
                writeSync(playerId, created);
                return created;
            }
        }, io);
    }

    @Override
    public CompletableFuture<Boolean> save(UUID playerId, PlayerBalanceData data) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (lock(playerId)) {
                cache.put(playerId, data);
                return writeSync(playerId, data);
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
                boolean ok = true;

                if (f.exists()) {
                    try {
                        ok = f.delete();
                        if (!ok) {
                            plugin.getLogger().atSevere().log("Could not delete balances file for " + playerId);
                        }
                    } catch (SecurityException e) {
                        plugin.getLogger().atSevere().log("Could not delete balances for " + playerId, e);
                        ok = false;
                    }
                }

                // Optional cleanup to prevent unbounded growth
                locks.remove(playerId);

                return ok;
            }
        }, io);
    }

    @Override
    public CompletableFuture<PlayerBalanceData> update(UUID playerId, UnaryOperator<PlayerBalanceData> mutator) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (lock(playerId)) {
                // Load current state (prefer cache)
                PlayerBalanceData current = cache.get(playerId);

                if (current == null) {
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

                // Apply mutation
                PlayerBalanceData mutated;
                try {
                    mutated = mutator.apply(current);
                } catch (Exception ex) {
                    plugin.getLogger().atSevere().log("Balance mutator failed for " + playerId, ex);
                    // keep current unchanged
                    mutated = current;
                }

                if (mutated == null) mutated = current;

                // Cache + persist
                cache.put(playerId, mutated);
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
