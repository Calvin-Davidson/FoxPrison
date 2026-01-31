package nl.trifox.foxprison.modules.economy.command.admin;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

public class EcoAdminCommand extends AbstractCommandCollection {

    public EcoAdminCommand() {
        super("eco", "Economy administration commands");
        this.addAliases("economy", "ecoadmin");
        this.setPermissionGroup(null); // Admin only - requires ecotale.ecotale.command.eco permission

        this.addSubCommand(new EcoSetCommand());
        this.addSubCommand(new EcoGiveCommand());
        this.addSubCommand(new EcoTakeCommand());
        this.addSubCommand(new EcoResetCommand());
        this.addSubCommand(new EcoSaveCommand());
    }
}