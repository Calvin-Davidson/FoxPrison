package nl.trifox.foxprison;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;

import nl.trifox.foxprison.commands.FoxPrisonCommand;
import nl.trifox.foxprison.commands.player.MineCommand;
import nl.trifox.foxprison.commands.player.RankUpCommand;
import nl.trifox.foxprison.commands.player.SellAllCommand;

import nl.trifox.foxprison.config.CoreConfig;
import nl.trifox.foxprison.config.EconomyConfig;
import nl.trifox.foxprison.config.MinesConfig;
import nl.trifox.foxprison.config.RanksConfig;

import nl.trifox.foxprison.data.InMemoryPlayerDataStore;
import nl.trifox.foxprison.data.PlayerDataStore;
import nl.trifox.foxprison.economy.Economy;
import nl.trifox.foxprison.economy.TheEconomyAdapter;
import nl.trifox.foxprison.events.MineBlockBreakEvent;
import nl.trifox.foxprison.service.MineService;
import nl.trifox.foxprison.service.RankService;

import javax.annotation.Nonnull;

public class FoxPrisonPlugin extends JavaPlugin {

    private final Config<CoreConfig> coreConfig;
    private final Config<EconomyConfig> economyConfig;
    private final Config<RanksConfig> ranksConfig;
    private final Config<MinesConfig> minesConfig;

    // Services (simple wiring)
    private PlayerDataStore dataStore;
    private Economy economy;
    private MineService mineService;
    private RankService rankService;

    public FoxPrisonPlugin(@Nonnull JavaPluginInit init) {
        super(init);

        this.coreConfig = withConfig("Core", CoreConfig.CODEC);
        this.economyConfig = withConfig("Economy", EconomyConfig.CODEC);
        this.ranksConfig = withConfig("Ranks", RanksConfig.CODEC);
        this.minesConfig = withConfig("Mines", MinesConfig.CODEC);
    }

    @Override
    protected void start() {
        mineService.startAutoResetLoop(getTaskRegistry());
    }

    @Override
    protected void setup() {
        // Basic storage (swap later for persistent)
        this.dataStore = new InMemoryPlayerDataStore();

        // Economy abstraction (start with TheEconomy)
        this.economy = new TheEconomyAdapter(this);

        this.mineService = new MineService(this, dataStore, economy, coreConfig, economyConfig, ranksConfig, minesConfig);
        this.rankService = new RankService(this, dataStore, economy, coreConfig, economyConfig, ranksConfig, minesConfig);

        getCommandRegistry().registerCommand(new FoxPrisonCommand(this, mineService));
        getCommandRegistry().registerCommand(new RankUpCommand(mineService, rankService));
        getCommandRegistry().registerCommand(new MineCommand(mineService));
        getCommandRegistry().registerCommand(new SellAllCommand(mineService));

        this.getEntityStoreRegistry().registerSystem(new MineBlockBreakEvent(mineService));

        getLogger().atInfo().log("FoxPrison setup complete.");
    }

    public Config<CoreConfig> getCoreConfig() { return coreConfig; }
    public Config<EconomyConfig> getEconomyConfig() { return economyConfig; }
    public Config<RanksConfig> getRanksConfig() { return ranksConfig; }
    public Config<MinesConfig> getMinesConfig() { return minesConfig; }
}
