package nl.trifox.foxprison;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;

import nl.trifox.foxprison.modules.economy.EconomyModule;
import nl.trifox.foxprison.modules.mines.MinesModule;
import nl.trifox.foxprison.modules.ranks.RankModule;

import nl.trifox.foxprison.framework.config.CoreConfig;
import nl.trifox.foxprison.modules.mines.config.MinesConfig;
import nl.trifox.foxprison.modules.ranks.config.RanksConfig;

import javax.annotation.Nonnull;

public class FoxPrisonPlugin extends JavaPlugin {

    private static EconomyModule economyModule;
    private static RankModule rankModule;
    private static MinesModule mineModule;

    private final Config<CoreConfig> coreConfig;
    private final Config<RanksConfig> ranksConfig;
    private final Config<MinesConfig> minesConfig;

    private static FoxPrisonPlugin instance;

    public FoxPrisonPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;

        this.coreConfig = withConfig("Core", CoreConfig.CODEC);
        this.ranksConfig = withConfig("Ranks", RanksConfig.CODEC);
        this.minesConfig = withConfig("Mines", MinesConfig.CODEC);
    }

    public static FoxPrisonPlugin getInstance() {
        return instance;
    }

    public static RankModule getRankModule() {
        return rankModule;
    }

    public static EconomyModule getEconomyModule() {
        return economyModule;
    }

    @Override
    protected void start() {
        mineModule.Start();
    }

    @Override
    protected void setup() {
        economyModule = new EconomyModule();
        rankModule = new RankModule();
        mineModule = new MinesModule();

        getLogger().atInfo().log("FoxPrison setup complete.");
    }

    public Config<MinesConfig> getMinesConfig() {
        return minesConfig;
    }

    public Config<RanksConfig> getRanksConfig() {
        return ranksConfig;
    }

    public Config<CoreConfig> getCoreConfig() {
        return coreConfig;
    }
}
