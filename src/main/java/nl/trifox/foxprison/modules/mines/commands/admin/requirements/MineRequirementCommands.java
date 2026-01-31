package nl.trifox.foxprison.modules.mines.commands.admin.requirements;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import nl.trifox.foxprison.modules.mines.MineService;
import nl.trifox.foxprison.modules.ranks.RankService;

public class MineRequirementCommands extends AbstractCommandCollection {

    public MineRequirementCommands(MineService mineService, RankService rankService) {
        super("requirement", "allows editing mine requirements");

        addSubCommand(new MineSetRankRequirement(mineService, rankService));
    }
}
