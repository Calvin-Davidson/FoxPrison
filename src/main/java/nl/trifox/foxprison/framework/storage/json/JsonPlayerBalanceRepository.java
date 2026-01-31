package nl.trifox.foxprison.framework.storage.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import nl.trifox.foxprison.FoxPrisonPlugin;
import nl.trifox.foxprison.framework.storage.repositories.PlayerBalanceRepository;
import nl.trifox.foxprison.modules.economy.data.PlayerBalanceData;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.UnaryOperator;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.concurrent.ConcurrentHashMap;

public final class JsonPlayerBalanceRepository implements PlayerBalanceRepository, AutoCloseable {

    private final FoxPrisonPlugin plugin;
    private final Path folder;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private final ConcurrentHashMap<UUID, PlayerBalanceData> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Object> locks = new ConcurrentHashMap<>();

    private final ExecutorService io =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "FoxPrison-IO-Balances");
                t.setDaemon(true);
                return t;
            });

    public JsonPlayerBalanceRepository(FoxPrisonPlugin plugin, java.io.File folder) {
        this.plugin = plugin;
        this.folder = folder.toPath();
    }

    private Object lock(UUID id) {
        return locks.computeIfAbsent(id, ignored -> new Object());
    }

    private Path file(UUID id) {
        return folder.resolve(id.toString() + ".json");
    }

    private void ensureFolder() throws IOException {
        if (Files.exists(folder)) return;
        Files.createDirectories(folder);
    }

    /**
     * Atomic write: write to temp, then move into place.
     * Prevents corrupted JSON if the process crashes mid-write.
     */
    private boolean writeSync(UUID playerId, PlayerBalanceData data) {
        try {
            ensureFolder();

            Path target = file(playerId);
            Path tmp = folder.resolve(playerId.toString() + ".json.tmp");

            try (var writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                gson.toJson(data, writer);
            }

            try {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                // Fallback if filesystem doesn't support ATOMIC_MOVE
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }

            return true;
        } catch (IOException e) {
            plugin.getLogger().atSevere().log("Could not save balances for " + playerId, e);
            return false;
        }
    }

    private PlayerBalanceData readSync(UUID playerId) {
        Path f = file(playerId);
        if (!Files.exists(f)) return null;

        try (var reader = Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
            PlayerBalanceData loaded = gson.fromJson(reader, PlayerBalanceData.class);
            return loaded != null ? loaded : new PlayerBalanceData();
        } catch (IOException e) {
            plugin.getLogger().atSevere().log("Could not read balances for " + playerId, e);
            return new PlayerBalanceData();
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

                PlayerBalanceData loaded = readSync(playerId);
                if (loaded != null) {
                    cache.put(playerId, loaded);
                    return loaded;
                }

                // Create new + persist
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
                // IMPORTANT:
                // If an instance is already cached (likely the live object),
                // do NOT replace it with 'data' (might be a snapshot).
                cache.putIfAbsent(playerId, data);
                return writeSync(playerId, data);
            }
        }, io);
    }

    @Override
    public CompletableFuture<Boolean> delete(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (lock(playerId)) {
                cache.remove(playerId);

                boolean ok = true;
                Path f = file(playerId);

                try {
                    Files.deleteIfExists(f);
                } catch (IOException | SecurityException e) {
                    plugin.getLogger().atSevere().log("Could not delete balances for " + playerId, e);
                    ok = false;
                }

                locks.remove(playerId);
                return ok;
            }
        }, io);
    }

    @Override
    public CompletableFuture<PlayerBalanceData> update(UUID playerId, UnaryOperator<PlayerBalanceData> mutator) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (lock(playerId)) {
                PlayerBalanceData current = cache.get(playerId);
                if (current == null) {
                    current = readSync(playerId);
                    if (current == null) current = new PlayerBalanceData();
                }

                PlayerBalanceData mutated;
                try {
                    mutated = mutator.apply(current);
                } catch (Exception ex) {
                    plugin.getLogger().atSevere().log("Balance mutator failed for " + playerId, ex);
                    mutated = current;
                }

                if (mutated == null) mutated = current;

                cache.put(playerId, mutated);
                writeSync(playerId, mutated);

                return mutated;
            }
        }, io);
    }

    @Override
    public void close() {
        io.shutdown();
        try {
            if (!io.awaitTermination(3, TimeUnit.SECONDS)) {
                io.shutdownNow();
            }
        } catch (InterruptedException e) {
            io.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
