package nl.trifox.foxprison.modules.economy.event;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import nl.trifox.foxprison.FoxPrisonPlugin;

import java.util.concurrent.CompletableFuture;

public class PlayerEvents {

    public static CompletableFuture<Void> onPlayerReady(PlayerReadyEvent event) {
        Ref<EntityStore> entityRef = event.getPlayerRef();
        Store<EntityStore> store = entityRef.getStore();

        PlayerRef playerRef = store.getComponent(entityRef, PlayerRef.getComponentType());
        if (playerRef == null) return CompletableFuture.completedFuture(null);

        return FoxPrisonPlugin.getEconomyModule().getEconomyManager().ensureAccountAsync(playerRef.getUuid()).thenApply(_ -> null);
    }

    public static void onPlayerQuit(PlayerDisconnectEvent playerDisconnectEvent) {
    }
}