package nl.trifox.foxprison.modules.ranks.commands.admin;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import nl.trifox.foxprison.config.RankDefinition;
import nl.trifox.foxprison.service.RankService;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.concurrent.CompletableFuture;

public class RankListCommand extends AbstractCommand {


    private final RankService rankService;

    protected RankListCommand(RankService rankService) {
        super("list", "list all existing ranks");
        this.rankService = rankService;
    }

    @NullableDecl
    @Override
    protected CompletableFuture<Void> execute(@NonNullDecl CommandContext commandContext) {
        var ranks = rankService.getAllRanks();
        StringBuilder builder = new StringBuilder("Ranks");
        for (RankDefinition rank : ranks) {
            builder.append("\n");
            builder.append(rank.getDisplayName());
        }

        commandContext.sender().sendMessage(Message.raw(builder.toString()));
        return CompletableFuture.completedFuture(null);
    }
}
