package nl.trifox.foxprison.modules.mines;

import nl.trifox.foxprison.FoxPrisonPlugin;
import nl.trifox.foxprison.api.interfaces.PlayerRankService;
import nl.trifox.foxprison.framework.module.FoxModule;
import nl.trifox.foxprison.modules.mines.commands.admin.MineCommands;
import nl.trifox.foxprison.modules.mines.commands.player.MineCommand;
import nl.trifox.foxprison.modules.mines.events.MineBlockBreakEvent;
import nl.trifox.foxprison.modules.ranks.RankModule;
import nl.trifox.foxprison.modules.ranks.commands.player.RankUpCommand;

public final class MinesModule implements FoxModule {

    private final FoxPrisonPlugin plugin;
    private final RankModule rankModule;
    private final PlayerRankService playerRankService;

    private MineService mineService;

    public MinesModule(FoxPrisonPlugin plugin, RankModule rankModule, PlayerRankService playerRankService) {
        this.plugin = plugin;
        this.rankModule = rankModule;
        this.playerRankService = playerRankService;
    }

    @Override
    public void start() {
        this.mineService = new MineService(plugin.getCoreConfig(), plugin.getMinesConfig(), playerRankService);
        plugin.getEntityStoreRegistry().registerSystem(new MineBlockBreakEvent(mineService));

        mineService.startAutoResetLoop(plugin.getTaskRegistry());

        var ranks = rankModule.getRankService();

        var registry = plugin.getCommandRegistry();
        registry.registerCommand(new MineCommand(mineService));
        registry.registerCommand(new MineCommands(mineService, ranks));

        registry.registerCommand(new RankUpCommand(ranks));
    }

    @Override
    public void stop() {

    }

    public MineService getMineService() {
        return mineService;
    }
}
