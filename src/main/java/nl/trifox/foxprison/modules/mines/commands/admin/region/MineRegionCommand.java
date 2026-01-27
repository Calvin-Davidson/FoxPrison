package nl.trifox.foxprison.modules.mines.commands.admin.region;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import nl.trifox.foxprison.service.MineService;

public class MineRegionCommand extends AbstractCommandCollection {

    public MineRegionCommand(MineService service) {
        super("region", "Mine region management");

        requirePermission("nl.trifox.foxprison.admin");

        addSubCommand(new RegionAddCommand(service));
        addSubCommand(new RegionClearCommand(service));
    }
}
