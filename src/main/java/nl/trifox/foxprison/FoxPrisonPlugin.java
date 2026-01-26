package nl.trifox.foxprison;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;

import net.cfh.vault.VaultUnlockedServicesManager;
import nl.trifox.foxprison.commands.FoxPrisonCommand;
import nl.trifox.foxprison.commands.economy.BalanceCommand;
import nl.trifox.foxprison.commands.economy.admin.EcoAdminCommand;
import nl.trifox.foxprison.commands.player.MineCommand;
import nl.trifox.foxprison.commands.player.RankCommand;
import nl.trifox.foxprison.commands.player.RankUpCommand;
import nl.trifox.foxprison.commands.player.SellAllCommand;

import nl.trifox.foxprison.config.CoreConfig;
import nl.trifox.foxprison.config.mines.MinesConfig;
import nl.trifox.foxprison.config.RanksConfig;

import nl.trifox.foxprison.data.player.JsonPlayerDataStore;
import nl.trifox.foxprison.data.player.PlayerDataStore;
import nl.trifox.foxprison.economy.EconomyManager;
import nl.trifox.foxprison.economy.VaultUnlockedEconomy;
import nl.trifox.foxprison.events.MineBlockBreakEvent;
import nl.trifox.foxprison.service.MineService;
import nl.trifox.foxprison.service.RankService;

import javax.annotation.Nonnull;

public class FoxPrisonPlugin extends JavaPlugin {

    private final Config<CoreConfig> coreConfig;
    private final Config<RanksConfig> ranksConfig;
    private final Config<MinesConfig> minesConfig;

    private final PlayerDataStore dataStore;
    private EconomyManager economy;
    private MineService mineService;
    private RankService rankService;

    private static FoxPrisonPlugin instance;

    public FoxPrisonPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;

        this.coreConfig = withConfig("Core", CoreConfig.CODEC);
        this.ranksConfig = withConfig("Ranks", RanksConfig.CODEC);
        this.minesConfig = withConfig("Mines", MinesConfig.CODEC);

        this.dataStore = new JsonPlayerDataStore(this, init.getDataDirectory().resolve("PlayerPrisonData").toFile());
    }

    @Override
    protected void start() {
        mineService.startAutoResetLoop(getTaskRegistry());
    }

    @Override
    protected void setup() {
        try {
            this.economy = new EconomyManager(this);
            if (this.getCoreConfig().get().isEnableEconomy()) {
                VaultUnlockedServicesManager.get().economy(new VaultUnlockedEconomy());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        this.mineService = new MineService(this, dataStore, economy, coreConfig, ranksConfig, minesConfig);
        this.rankService = new RankService(this, dataStore, economy, coreConfig, ranksConfig, minesConfig);

        getCommandRegistry().registerCommand(new FoxPrisonCommand(this, mineService, rankService));
        getCommandRegistry().registerCommand(new RankUpCommand(mineService, rankService));
        getCommandRegistry().registerCommand(new MineCommand(mineService));
        getCommandRegistry().registerCommand(new SellAllCommand(mineService));
        getCommandRegistry().registerCommand(new RankCommand(rankService));

        if (this.getCoreConfig().get().isEnableEconomy()) {
            getCommandRegistry().registerCommand(new EcoAdminCommand());
            getCommandRegistry().registerCommand(new BalanceCommand());
        }

        this.getEntityStoreRegistry().registerSystem(new MineBlockBreakEvent(mineService));

        getLogger().atInfo().log("FoxPrison setup complete.");
    }

    public Config<CoreConfig> getCoreConfig() { return coreConfig; }
    public Config<RanksConfig> getRanksConfig() { return ranksConfig; }
    public Config<MinesConfig> getMinesConfig() { return minesConfig; }

    public EconomyManager getEconomy() {
        return economy;
    }

    public static FoxPrisonPlugin getInstance() {
        return instance;
    }
}
