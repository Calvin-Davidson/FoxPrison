package nl.trifox.foxprison.modules.ranks.commands.player;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import nl.trifox.foxprison.modules.ranks.RankService;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.concurrent.CompletableFuture;

public class RankCommand extends AbstractAsyncPlayerCommand {
    private final RankService service;

    public RankCommand(RankService service) {
        super("rank", "get's info about your current rank");
        this.service = service;
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(@NonNullDecl CommandContext commandContext, @NonNullDecl Store<EntityStore> store, @NonNullDecl Ref<EntityStore> ref, @NonNullDecl PlayerRef playerRef, @NonNullDecl World world) {
        return service.getRankIdByPlayer(playerRef).thenCompose(rankID -> {
            var optionalRank = service.getRank(rankID);
            if (optionalRank.isEmpty()) {
                playerRef.sendMessage(Message.raw("your rank does no longer exist"));
                return CompletableFuture.completedFuture(null);
            }

            playerRef.sendMessage(Message.raw("You are rank: " + optionalRank.get().getDisplayName()));
            return CompletableFuture.completedFuture(null);
        });
    }
}
