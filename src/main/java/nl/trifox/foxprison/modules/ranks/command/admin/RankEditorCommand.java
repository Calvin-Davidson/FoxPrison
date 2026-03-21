package nl.trifox.foxprison.modules.ranks.command.admin;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import nl.trifox.foxprison.FoxPrisonPlugin;
import nl.trifox.foxprison.modules.ranks.RankService;
import nl.trifox.foxprison.modules.ranks.ui.RankEditorIndexPage;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.concurrent.CompletableFuture;

/**
 * /rankeditor — opens a GUI for viewing all ranks and editing them.
 */
public class RankEditorCommand extends AbstractAsyncPlayerCommand {

    private final FoxPrisonPlugin plugin;
    private final RankService rankService;

    public RankEditorCommand(FoxPrisonPlugin plugin, RankService rankService) {
        super("rankeditor", "Opens the rank editor GUI");
        this.plugin = plugin;
        this.rankService = rankService;
        requirePermission("foxprison.rank.command.admin.rankeditor");
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(@NonNullDecl CommandContext context,
                                                   @NonNullDecl Store<EntityStore> store,
                                                   @NonNullDecl Ref<EntityStore> ref,
                                                   @NonNullDecl PlayerRef playerRef,
                                                   @NonNullDecl World world) {

        if (!(context.sender() instanceof Player player)) {
            context.sender().sendMessage(Message.raw("This command must be run by a player."));
            return CompletableFuture.completedFuture(null);
        }

        var page = new RankEditorIndexPage(playerRef, player, plugin, rankService);
        player.getPageManager().openCustomPage(ref, store, page);

        return CompletableFuture.completedFuture(null);
    }
}

