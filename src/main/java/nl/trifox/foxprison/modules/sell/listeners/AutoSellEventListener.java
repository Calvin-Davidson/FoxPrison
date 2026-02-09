package nl.trifox.foxprison.modules.sell.listeners;

import nl.trifox.foxprison.api.events.mines.MineDropsEvent;
import nl.trifox.foxprison.modules.sell.PlayerAutoSellService;
import nl.trifox.foxprison.modules.sell.SellService;

public class AutoSellEventListener {

    private final SellService sellService;
    private final PlayerAutoSellService playerAutoSellService;

    public AutoSellEventListener(SellService sellService, PlayerAutoSellService playerAutoSellService) {
        this.sellService = sellService;
        this.playerAutoSellService = playerAutoSellService;
    }

    public void handleMineDrops(MineDropsEvent event) {
        if (event.isCancelled()) return;

        if (!playerAutoSellService.isEnabled(event.getPlayerUuid())) return;

        boolean soldSomething = sellService.autoSell(event.getPlayerUuid(), event.getDrops());
        if (!soldSomething) return;

        event.getDrops().clear(); // auto sold
    }
}
