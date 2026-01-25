package nl.trifox.foxprison.commands.admin.mine.autoreset;

import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import nl.trifox.foxprison.service.MineService;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class MineAutoResetCommands extends AbstractCommandCollection {

    public MineAutoResetCommands(MineService service) {
        super("autoreset", "modify autoreset settings");

        addSubCommand(new MineAutoResetSetCommand(service));
    }
}
