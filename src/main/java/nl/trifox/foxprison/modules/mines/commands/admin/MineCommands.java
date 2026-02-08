package nl.trifox.foxprison.modules.mines.commands.admin;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import nl.trifox.foxprison.modules.mines.commands.admin.autoreset.MineAutoResetCommands;
import nl.trifox.foxprison.modules.mines.commands.admin.region.MineRegionCommand;
import nl.trifox.foxprison.modules.mines.commands.admin.requirements.MineRequirementCommands;
import nl.trifox.foxprison.modules.mines.MineService;
import nl.trifox.foxprison.modules.ranks.RankService;

public class MineCommands extends AbstractCommandCollection {
    public MineCommands(MineService mineService, RankService rankService) {
        super("mine", "Mine management");
        requirePermission("foxprison.mine.command.admin");

        addSubCommand(new MineRegionCommand(mineService));
        addSubCommand(new MineCreateCommand(mineService));
        addSubCommand(new MineDeleteCommand(mineService));
        addSubCommand(new MineResetCommand(mineService));
        addSubCommand(new MineSetBlockPatternCommand(mineService));
        addSubCommand(new MineTpCommand(mineService));
        addSubCommand(new MineListCommand(mineService));
        addSubCommand(new MineRequirementCommands(mineService, rankService));
        addSubCommand(new MineAutoResetCommands(mineService));
        addSubCommand(new MineSetSpawn(mineService));
    }
}
