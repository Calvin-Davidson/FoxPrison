package nl.trifox.foxprison.modules.sell;

import nl.trifox.foxprison.modules.economy.EconomyManager;
import nl.trifox.foxprison.modules.sell.config.SellConfig;
import nl.trifox.foxprison.modules.sell.config.SellPriceDefinition;

import com.hypixel.hytale.server.core.inventory.ItemStack;

import java.util.*;

public final class SellService {

    private final EconomyManager economyManager;
    private final SellConfig sellConfig;

    public SellService(EconomyManager economyManager, SellConfig sellConfig) {
        this.economyManager = economyManager;
        this.sellConfig = sellConfig;
    }

    /**
     * Autosell stacks from the given drops list.
     * - computes totals
     * - deposits per currency
     * - removes sold stacks from drops (in-place) ONLY if deposits succeeded
     *
     * @return true if at least 1 stack was sold
     */
    public boolean autoSell(UUID playerUuid, List<ItemStack> drops) {
        if (drops == null || drops.isEmpty()) return false;
        if (!sellConfig.isEnabled() || !sellConfig.isSellEnabled()) return false;

        Map<String, Double> totalsByCurrency = new HashMap<>();
        List<Integer> sellIndexes = new ArrayList<>();

        for (int idx = 0; idx < drops.size(); idx++) {
            ItemStack stack = drops.get(idx);
            if (stack == null || stack.isEmpty()) continue;

            String itemId = stack.getItemId();
            int qty = stack.getQuantity();
            if (qty <= 0) continue;

            SellPriceDefinition price = sellConfig.getPriceForItemId(itemId);
            if (price == null) continue;


            if (!price.isAllowSellAll()) continue;
            if (price.getPriceEach() <= 0.0) continue;

            double total = price.getPriceEach() * qty;
            if (total <= 0.0) continue;

            totalsByCurrency.merge(price.getCurrency(), total, Double::sum);
            sellIndexes.add(idx);
        }

        if (sellIndexes.isEmpty()) return false;

        for (var entry : totalsByCurrency.entrySet()) {
            String currency = entry.getKey();
            double amount = entry.getValue();
            boolean ok = economyManager.deposit(playerUuid, amount, "autosell", currency);
            if (!ok) return false;
        }

        return true;
    }
}
