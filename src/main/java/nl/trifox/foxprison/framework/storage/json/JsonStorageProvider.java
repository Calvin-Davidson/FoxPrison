package nl.trifox.foxprison.framework.storage.json;

import nl.trifox.foxprison.FoxPrisonPlugin;
import nl.trifox.foxprison.framework.storage.StorageProvider;
import nl.trifox.foxprison.framework.storage.repositories.PlayerBalanceRepository;
import nl.trifox.foxprison.framework.storage.repositories.PlayerRankRepository;

import java.io.File;

public final class JsonStorageProvider implements StorageProvider {

    private final FoxPrisonPlugin plugin;
    private final File baseFolder;

    private PlayerBalanceRepository balances;
    private PlayerRankRepository ranks;

    public JsonStorageProvider(FoxPrisonPlugin plugin, File baseFolder) {
        this.plugin = plugin;
        this.baseFolder = baseFolder;
    }

    @Override
    public void init() {
        // Ensure folders exist
        if (!baseFolder.exists()) baseFolder.mkdirs();

        File balancesDir = new File(baseFolder, "balances");
        File ranksDir = new File(baseFolder, "ranks");
        balancesDir.mkdirs();
        ranksDir.mkdirs();

        this.balances = new JsonPlayerBalanceRepository(plugin, balancesDir);
        this.ranks = new JsonPlayerRankRepository(plugin, ranksDir);
    }

    @Override
    public PlayerBalanceRepository balances() {
        return balances;
    }

    @Override
    public PlayerRankRepository ranks() {
        return ranks;
    }

    @Override
    public String getName() {
        return "json";
    }

    @Override
    public void close() {
        // Repos hold their own executors; close them if you want (shown below)
        if (balances instanceof AutoCloseable c) {
            try { c.close(); } catch (Exception ignored) {}
        }
        if (ranks instanceof AutoCloseable c) {
            try { c.close(); } catch (Exception ignored) {}
        }
    }
}