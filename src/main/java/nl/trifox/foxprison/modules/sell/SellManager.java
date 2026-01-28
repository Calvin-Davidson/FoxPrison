package nl.trifox.foxprison.modules.sell;

import nl.trifox.foxprison.modules.sell.commands.admin.SellAdminCommands;
import nl.trifox.foxprison.modules.sell.config.SellPriceDefinition;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

public interface SellManager {
    /**
     * @return a sell definition for an itemId, or null if not sellable
     */
    SellPriceDefinition getPrice(@Nonnull String itemId);

    /**
     * Upsert a price definition for itemId.
     */
    void setPrice(@Nonnull String itemId, @Nonnull SellPriceDefinition def);

    /**
     * Persist changes (optional but recommended).
     */
    CompletableFuture<Void> saveAsync();
}
