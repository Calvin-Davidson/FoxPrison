package nl.trifox.foxprison.modules.mines.commands.admin;

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
import nl.trifox.foxprison.modules.mines.MineService;
import nl.trifox.foxprison.modules.mines.ui.MineEditorIndexPage;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.concurrent.CompletableFuture;

/**
 * /mineeditor — opens a GUI for viewing all mines and editing them.
 */
public class MineEditorCommand extends AbstractAsyncPlayerCommand {

    private final FoxPrisonPlugin plugin;
    private final MineService mineService;

    public MineEditorCommand(FoxPrisonPlugin plugin, MineService mineService) {
        super("mineeditor", "Opens the mine editor GUI");
        this.plugin = plugin;
        this.mineService = mineService;
        requirePermission("foxprison.mine.command.admin.mineeditor");
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

        var page = new MineEditorIndexPage(playerRef, player, plugin, mineService);
        player.getPageManager().openCustomPage(ref, store, page);

        return CompletableFuture.completedFuture(null);
    }
}

