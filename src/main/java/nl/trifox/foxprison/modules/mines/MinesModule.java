package nl.trifox.foxprison.modules.mines;

import nl.trifox.foxprison.FoxPrisonPlugin;
import nl.trifox.foxprison.modules.mines.events.MineBlockBreakEvent;

public class MinesModule {

    private final FoxPrisonPlugin foxPrisonPlugin = FoxPrisonPlugin.getInstance();

    private final MineService mineService;

    public MinesModule() {
        mineService = new MineService(foxPrisonPlugin.getCoreConfig(), foxPrisonPlugin.getMinesConfig());
        foxPrisonPlugin.getEntityStoreRegistry().registerSystem(new MineBlockBreakEvent(mineService));
    }

    public void Start() {
        mineService.startAutoResetLoop(foxPrisonPlugin.getTaskRegistry());
    }
}
