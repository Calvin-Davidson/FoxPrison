package nl.trifox.foxprison.modules.ranks.commands.admin;

import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import nl.trifox.foxprison.modules.ranks.RankService;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.concurrent.CompletableFuture;

public class RankSetCommand extends AbstractCommand {
    private final RankService service;
    private final RequiredArg<PlayerRef> playerName;
    private final RequiredArg<String> targetRank;

    protected RankSetCommand(RankService service) {
        super("set", "set's the rank of an specific player");
        this.service = service;
        this.playerName = this.withRequiredArg("Player name", "The name of the place which you want to change the rank of", ArgTypes.PLAYER_REF);
        this.targetRank = this.withRequiredArg("rank", "the rank you wanna set", ArgTypes.STRING);
    }

    @NullableDecl
    @Override
    protected CompletableFuture<Void> execute(@NonNullDecl CommandContext commandContext) {
        var player = playerName.get(commandContext);

        return service.setRankByName(commandContext.sender(), player, targetRank.get(commandContext))
                .thenApply(_ -> null);
    }
}

