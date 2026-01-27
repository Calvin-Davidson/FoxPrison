package nl.trifox.foxprison.modules.mines;

import nl.trifox.foxprison.FoxPrisonPlugin;
import nl.trifox.foxprison.framework.module.FoxModule;
import nl.trifox.foxprison.modules.mines.commands.admin.MineCommands;
import nl.trifox.foxprison.modules.mines.commands.player.MineCommand;
import nl.trifox.foxprison.modules.mines.events.MineBlockBreakEvent;
import nl.trifox.foxprison.modules.ranks.RankModule;
import nl.trifox.foxprison.modules.ranks.commands.player.RankUpCommand;

public final class MinesModule implements FoxModule {

    private final FoxPrisonPlugin plugin;
    private final RankModule rankModule;
    private final MineService mineService;

    public MinesModule(FoxPrisonPlugin plugin, RankModule rankModule) {
        this.plugin = plugin;
        this.rankModule = rankModule;

        this.mineService = new MineService(plugin.getCoreConfig(), plugin.getMinesConfig());
        plugin.getEntityStoreRegistry().registerSystem(new MineBlockBreakEvent(mineService));
    }

    @Override
    public void start() {
        mineService.startAutoResetLoop(plugin.getTaskRegistry());

        var ranks = rankModule.getRankService();

        var registry = plugin.getCommandRegistry();
        registry.registerCommand(new MineCommand(mineService));
        registry.registerCommand(new MineCommands(mineService, ranks));


        registry.registerCommand(new RankUpCommand(mineService, ranks));
    }

    public MineService getMineService() {
        return mineService;
    }
}
