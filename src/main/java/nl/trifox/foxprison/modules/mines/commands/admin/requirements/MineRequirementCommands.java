package nl.trifox.foxprison.modules.mines.commands.admin.requirements;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import nl.trifox.foxprison.service.MineService;
import nl.trifox.foxprison.service.RankService;

public class MineRequirementCommands extends AbstractCommandCollection {

    public MineRequirementCommands(MineService mineService, RankService rankService) {
        super("requirement", "allows editing mine requirements");

        addSubCommand(new MineSetRankRequirement(mineService, rankService));
    }
}
