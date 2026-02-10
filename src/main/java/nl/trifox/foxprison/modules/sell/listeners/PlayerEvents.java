package nl.trifox.foxprison.modules.sell.listeners;

import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import nl.trifox.foxprison.modules.sell.PlayerAutoSellService;

public class PlayerEvents {

    private final PlayerAutoSellService playerAutoSellService;

    public PlayerEvents(PlayerAutoSellService playerAutoSellService) {
        this.playerAutoSellService = playerAutoSellService;
    }

    public void onPlayerQuit(PlayerDisconnectEvent playerDisconnectEvent) {
        playerAutoSellService.clear(playerDisconnectEvent.getPlayerRef().getUuid());
    }
}