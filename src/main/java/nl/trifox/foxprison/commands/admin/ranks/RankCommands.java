package nl.trifox.foxprison.commands.admin.ranks;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import nl.trifox.foxprison.service.RankService;

public class RankCommands extends AbstractCommandCollection {

    public RankCommands(RankService rankService) {
        super("rank", "rank management");
        requirePermission("nl.trifox.foxprison.admin");

        addSubCommand(new RankListCommand(rankService));
        addSubCommand(new RankSetCommand(rankService));
    }

}
