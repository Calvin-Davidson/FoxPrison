package nl.trifox.foxprison.data.player;

import com.google.gson.Gson;
import nl.trifox.foxprison.FoxPrisonPlugin;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class JsonPlayerDataStore implements PlayerDataStore {

    private final ConcurrentHashMap<UUID, PlayerPrisonData> data = new ConcurrentHashMap<>();
    private final FoxPrisonPlugin plugin;
    private final File dataFolder;
    private final ExecutorService ioExecutor =
            Executors.newSingleThreadExecutor(r -> new Thread(r, "Prison-Data-IO"));


    public JsonPlayerDataStore(FoxPrisonPlugin plugin, File dataFolder) {
        this.plugin = plugin;
        this.dataFolder = dataFolder;
        var _ = this.dataFolder.mkdirs();
    }

    @Override
    public CompletableFuture<PlayerPrisonData> getOrCreate(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            File file = new File(dataFolder, playerId.toString() + ".json");

            if (!file.exists()) {
                PlayerPrisonData data = new PlayerPrisonData();
                save(playerId, data); // optional: immediately persist
                return data;
            }

            try (FileReader reader = new FileReader(file)) {
                return new Gson().fromJson(reader, PlayerPrisonData.class);
            } catch (IOException e) {
                plugin.getLogger().atSevere()
                        .log("Could not read " + playerId + " prison data", e);
                return new PlayerPrisonData();
            }
        }, ioExecutor);
    }


    @Override
    public CompletableFuture<Boolean> save(UUID playerId, PlayerPrisonData data) {
        return CompletableFuture.supplyAsync(() -> {
            File file = new File(dataFolder, playerId.toString() + ".json");

            try {
                // Ensure data folder exists
                if (!dataFolder.exists() && !dataFolder.mkdirs()) {
                    plugin.getLogger().atSevere()
                            .log("Could not create data folder for prison data");
                    return false;
                }

                try (FileWriter writer = new FileWriter(file)) {
                    new Gson().toJson(data, writer);
                }

                return true;
            } catch (IOException e) {
                plugin.getLogger().atSevere()
                        .log("Could not save prison data for " + playerId, e);
                return false;
            }
        }, ioExecutor);
    }
}
