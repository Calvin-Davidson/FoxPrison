package nl.trifox.foxprison.commands.admin.mine;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import nl.trifox.foxprison.commands.admin.mine.region.MineRegionCommand;
import nl.trifox.foxprison.service.MineService;

public class MineCommand extends AbstractCommandCollection {
    public MineCommand(MineService service) {
        super("mine", "Mine management");
        requirePermission("nl.trifox.foxprison.admin");

        addSubCommand(new MineRegionCommand(service));
        addSubCommand(new MineCreateCommand(service));
        addSubCommand(new MineDeleteCommand(service));
        addSubCommand(new MineResetCommand(service));
        addSubCommand(new MineSetBlockPatternCommand(service));
        addSubCommand(new MineTpCommand(service));
        addSubCommand(new MineListCommand(service));
    }
}
