package nl.trifox.foxprison.modules.mines.commands.admin.autoreset;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import nl.trifox.foxprison.service.MineService;

public class MineAutoResetCommands extends AbstractCommandCollection {

    public MineAutoResetCommands(MineService service) {
        super("autoreset", "modify autoreset settings");

        addSubCommand(new MineAutoResetSetCommand(service));
    }
}
