package nl.trifox.foxprison.commands;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

import nl.trifox.foxprison.FoxPrisonPlugin;

import nl.trifox.foxprison.commands.admin.mine.MineCommand;
import nl.trifox.foxprison.commands.admin.mine.MineListCommand;
import nl.trifox.foxprison.commands.admin.ranks.RankCommands;
import nl.trifox.foxprison.service.MineService;
import nl.trifox.foxprison.service.RankService;

public class FoxPrisonCommand extends AbstractCommandCollection {

    public FoxPrisonCommand(FoxPrisonPlugin plugin, MineService service, RankService rankService) {
        super("foxprison", "FoxPrison admin commands");
        addAliases("fp");

        addSubCommand(new MineCommand(service, rankService));
        addSubCommand(new RankCommands(rankService));
    }
}
