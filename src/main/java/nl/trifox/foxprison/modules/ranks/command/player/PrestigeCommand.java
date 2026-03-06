package nl.trifox.foxprison.modules.ranks.command.player;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import nl.trifox.foxprison.modules.ranks.RankService;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class PrestigeCommand extends AbstractAsyncPlayerCommand {

    private final RankService service;

    public PrestigeCommand(RankService service) {
        super("prestige", "Prestige after reaching max rank");
        this.service = service;
    }

    @Override
    protected @NotNull CompletableFuture<Void> executeAsync(@NotNull CommandContext commandContext, @NotNull Store<EntityStore> store, @NotNull Ref<EntityStore> ref, @NotNull PlayerRef playerRef, @NotNull World world) {
        return service.prestige(playerRef).thenApply(_ -> null);
    }
}