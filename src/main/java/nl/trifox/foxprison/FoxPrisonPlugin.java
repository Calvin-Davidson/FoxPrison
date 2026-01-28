package nl.trifox.foxprison;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;

import nl.trifox.foxprison.framework.storage.StorageModule;
import nl.trifox.foxprison.modules.economy.EconomyModule;
import nl.trifox.foxprison.modules.mines.MinesModule;
import nl.trifox.foxprison.modules.ranks.RankModule;

import nl.trifox.foxprison.framework.config.CoreConfig;
import nl.trifox.foxprison.modules.mines.config.MinesConfig;
import nl.trifox.foxprison.modules.ranks.config.RanksConfig;
import nl.trifox.foxprison.modules.sell.SellModule;
import nl.trifox.foxprison.modules.sell.config.SellConfig;

import javax.annotation.Nonnull;

public class FoxPrisonPlugin extends JavaPlugin {

    private static EconomyModule economyModule;
    private static RankModule rankModule;
    private static MinesModule mineModule;
    private static StorageModule storageModule;
    private SellModule sellModule;

    private final Config<CoreConfig> coreConfig;
    private final Config<RanksConfig> ranksConfig;
    private final Config<MinesConfig> minesConfig;
    private final Config<SellConfig> sellConfig;

    private static FoxPrisonPlugin instance;

    public FoxPrisonPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;

        this.coreConfig = withConfig("Core", CoreConfig.CODEC);
        this.ranksConfig = withConfig("Ranks", RanksConfig.CODEC);
        this.minesConfig = withConfig("Mines", MinesConfig.CODEC);
        this.sellConfig = withConfig("Sell", SellConfig.CODEC);
    }

    public static FoxPrisonPlugin getInstance() {
        return instance;
    }

    public static RankModule getRankModule() {
        return rankModule;
    }
    public static MinesModule getMineModule() {return mineModule;}

    public static EconomyModule getEconomyModule() {
        return economyModule;
    }

    public static StorageModule getStorageModule() {
        return storageModule;
    }

    @Override
    protected void start() {
        getLogger().atInfo().log("Plugin started!");
    }

    @Override
    protected void setup() {
        storageModule = new StorageModule(this, coreConfig.get());
        economyModule = new EconomyModule(this, storageModule);
        rankModule    = new RankModule(this, storageModule, economyModule);
        mineModule    = new MinesModule(this, rankModule);
        sellModule    = new SellModule(this, economyModule, sellConfig.get());

        storageModule.start();  // provider.init()
        economyModule.start();  // builds EconomyManager + registers Vault service + commands
        rankModule.start();     // builds RankService + registers rank commands
        mineModule.start();     // registers mine commands that need rank service, starts loops

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

    public SellModule getSellModule() {
        return sellModule;
    }

    public Config<SellConfig> getSellConfig() {
        return sellConfig;
    }
}
