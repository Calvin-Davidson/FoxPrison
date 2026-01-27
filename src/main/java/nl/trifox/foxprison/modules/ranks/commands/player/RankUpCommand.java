package nl.trifox.foxprison.modules.ranks.commands.player;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import nl.trifox.foxprison.modules.mines.MineService;
import nl.trifox.foxprison.modules.ranks.RankService;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.concurrent.CompletableFuture;

public class RankUpCommand extends AbstractAsyncPlayerCommand {

    private final RankService rankService;
    private final MineService mineService;

    public RankUpCommand(MineService service, RankService rankService) {
        super("rankup", "Rank up to the next rank");
        this.rankService = rankService;
        this.mineService = service;
        addAliases("ru");
        // No requirePermission(): let everyone use rankup (or add a player perm later)
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(@NonNullDecl CommandContext commandContext, @NonNullDecl Store<EntityStore> store, @NonNullDecl Ref<EntityStore> ref, @NonNullDecl PlayerRef playerRef, @NonNullDecl World world) {
        return rankService.rankup(playerRef).thenApply(_ -> null);
    }
}
