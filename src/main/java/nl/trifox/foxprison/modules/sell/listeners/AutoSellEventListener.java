package nl.trifox.foxprison.modules.sell.listeners;

import com.hypixel.hytale.server.core.inventory.ItemStack;
import nl.trifox.foxprison.api.events.mines.MineDropsEvent;
import nl.trifox.foxprison.modules.sell.SellService;
import nl.trifox.foxprison.modules.sell.config.AutoSellDefinition;

public class AutoSellEventListener {

    private final SellService sellService;

    public AutoSellEventListener(SellService sellService) {
        this.sellService = sellService;
    }

    public MineDropsEvent handleMineDrops(MineDropsEvent event) {
        if (event.isCancelled()) return event;

        var tool = event.getToolInHand();
        if (!hasAutoSell(tool)) return event;

        boolean soldSomething = sellService.autoSell(event.getPlayerUuid(), event.getDrops());
        if (!soldSomething) return event;

        event.getDrops().clear(); // auto sold
        return event;
    }

    private boolean hasAutoSell(ItemStack stack) {
        var autoSell = stack.getFromMetadataOrNull("AutoSell", AutoSellDefinition.CODEC);
        return autoSell != null && autoSell.isAutoSellEnabled();
    }
}
