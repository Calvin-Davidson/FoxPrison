package nl.trifox.foxprison.commands.admin.mine.requirements;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import nl.trifox.foxprison.service.MineService;
import nl.trifox.foxprison.service.RankService;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class MineRequirementCommands extends AbstractCommandCollection {

    public MineRequirementCommands(MineService mineService, RankService rankService) {
        super("requirement", "allows editing mine requirements");

        addSubCommand(new MineSetRankRequirement(mineService, rankService));
    }
}
