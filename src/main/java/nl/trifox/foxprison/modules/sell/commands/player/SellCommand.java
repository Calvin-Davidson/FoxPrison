package nl.trifox.foxprison.modules.sell.commands.player;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import nl.trifox.foxprison.FoxPrisonPlugin;
import nl.trifox.foxprison.modules.economy.EconomyManager;
import nl.trifox.foxprison.modules.sell.config.SellConfig;
import nl.trifox.foxprison.modules.sell.config.SellPriceDefinition;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackSlotTransaction;

import java.util.UUID;

public class SellCommand extends AbstractAsyncPlayerCommand {

    private final EconomyManager economyManager;
    private final SellConfig sellConfig;

    public SellCommand(EconomyManager economyManager, SellConfig sellConfig) {
        super("sell", "sell the item in your hand");
        this.economyManager = economyManager;
        this.sellConfig = sellConfig;
    }

    @Override
    protected @NotNull CompletableFuture<Void> executeAsync(
            @NotNull CommandContext commandContext,
            @NotNull Store<EntityStore> store,
            @NotNull Ref<EntityStore> ref,
            @NotNull PlayerRef playerRef,
            @NotNull World world
    ) {
        if (!sellConfig.isEnabled() || !sellConfig.isSellEnabled()) {
            playerRef.sendMessage(Message.raw("Selling is disabled."));
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<Void> done = new CompletableFuture<>();

        world.execute(() -> {
            try {
                Player player = store.getComponent(ref, Player.getComponentType());
                if (player == null) {
                    playerRef.sendMessage(Message.translation("You're not in a world."));
                    done.complete(null);
                    return;
                }

                Inventory inv = player.getInventory();
                ItemStack inHand = inv.getItemInHand();

                if (inHand == null || inHand.isEmpty()) {
                    playerRef.sendMessage(Message.translation("foxPrison.sell.no_item_in_hand"));
                    done.complete(null);
                    return;
                }

                String itemId = inHand.getItemId();
                int qty = inHand.getQuantity();

                SellPriceDefinition price = sellConfig.getPriceForItemId(itemId);
                if (price == null || !price.isAllowSell() || price.getPriceEach() <= 0.0) {
                    playerRef.sendMessage(Message.translation("sell.fail.single").param("item_id", itemId));
                    done.complete(null);
                    return;
                }

                double total = price.getPriceEach() * qty;
                if (total <= 0.0) {
                    playerRef.sendMessage(Message.translation("sell.fail.single").param("item_id", itemId));
                    done.complete(null);
                    return;
                }

                // Determine where "item in hand" came from so we can remove it.
                final boolean usingToolsItem = inv.usingToolsItem();
                final ItemContainer container;
                final short slot;

                if (usingToolsItem) {
                    container = inv.getTools();
                    slot = inv.getActiveToolsSlot();
                } else {
                    container = inv.getHotbar();
                    slot = inv.getActiveHotbarSlot();
                }

                ItemStackSlotTransaction tx = container.setItemStackForSlot(slot, ItemStack.EMPTY);
                if (!tx.succeeded()) {
                    playerRef.sendMessage(Message.translation("sell.fail.single").param("item_id", itemId));
                    done.complete(null);
                    return;
                }

                UUID uuid = playerRef.getUuid();
                var depositOk = economyManager.deposit(uuid, total, "sellhand", price.getCurrency());

                if (!depositOk) {
                    world.execute(() -> container.setItemStackForSlot(slot, inHand));
                    playerRef.sendMessage(Message.translation("foxPrison.sell.fail.single").param("item_id", inHand.getItemId()));
                    done.complete(null);
                    return;
                }

                var totalFormatted = FoxPrisonPlugin.getInstance().getEconomyConfig().get().getCurrency(price.getCurrency()).format(total);
                playerRef.sendMessage(Message.translation("foxPrison.sell.success.single")
                        .param("quantity", qty)
                        .param("item_id", itemId)
                        .param("currency_id", price.getCurrency())
                        .param("total", totalFormatted));

                done.complete(null);

            } catch (Throwable t) {
                done.completeExceptionally(t);
            }
        });

        return done;
    }
}

