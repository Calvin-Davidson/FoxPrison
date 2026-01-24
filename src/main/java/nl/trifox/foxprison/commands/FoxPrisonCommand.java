package nl.trifox.foxprison.commands;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

import nl.trifox.foxprison.FoxPrisonPlugin;

import nl.trifox.foxprison.commands.admin.mine.MineCommand;
import nl.trifox.foxprison.service.MineService;

public class FoxPrisonCommand extends AbstractCommandCollection {

    public FoxPrisonCommand(FoxPrisonPlugin plugin, MineService service) {
        super("foxprison", "FoxPrison admin commands");
        addAliases("fp");

        addSubCommand(new MineListCommand(service));
        addSubCommand(new MineCommand(service));
    }
}
