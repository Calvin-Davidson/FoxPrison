package nl.trifox.foxprison.commands.admin.mine;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import nl.trifox.foxprison.commands.admin.mine.autoreset.MineAutoResetCommands;
import nl.trifox.foxprison.commands.admin.mine.region.MineRegionCommand;
import nl.trifox.foxprison.commands.admin.mine.requirements.MineRequirementCommands;
import nl.trifox.foxprison.service.MineService;
import nl.trifox.foxprison.service.RankService;

public class MineCommand extends AbstractCommandCollection {
    public MineCommand(MineService mineService, RankService rankService) {
        super("mine", "Mine management");
        requirePermission("nl.trifox.foxprison.admin");

        addSubCommand(new MineRegionCommand(mineService));
        addSubCommand(new MineCreateCommand(mineService));
        addSubCommand(new MineDeleteCommand(mineService));
        addSubCommand(new MineResetCommand(mineService));
        addSubCommand(new MineSetBlockPatternCommand(mineService));
        addSubCommand(new MineTpCommand(mineService));
        addSubCommand(new MineListCommand(mineService));
        addSubCommand(new MineRequirementCommands(mineService, rankService));
        addSubCommand(new MineAutoResetCommands(mineService));
    }
}
