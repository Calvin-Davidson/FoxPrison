package nl.trifox.foxprison.modules.mines.commands.admin;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import nl.trifox.foxprison.config.mines.MineDefinition;
import nl.trifox.foxprison.service.MineService;

import javax.annotation.Nonnull;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;

public class MineListCommand extends AbstractCommand {

    private final MineService service;

    public MineListCommand(MineService service) {
        super("list", "List configured mines");
        requirePermission("nl.trifox.foxprison.admin");
        this.service = service;
        addAliases("mines");
    }

    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
        String mines = service.getAllMines().stream()
                .map(MineDefinition::getId)
                .collect(Collectors.joining(", "));

        context.sender().sendMessage(Message.raw("Mines: " + (mines.isBlank() ? "(none)" : mines)));
        return CompletableFuture.completedFuture(null);
    }
}
