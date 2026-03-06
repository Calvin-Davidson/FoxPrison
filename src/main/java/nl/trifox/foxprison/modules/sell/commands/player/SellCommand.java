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
import nl.trifox.foxprison.modules.ranks.RankService;
import nl.trifox.foxprison.modules.ranks.config.RanksConfig;
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

    private final RankService rankService;
    private final EconomyManager economyManager;
    private final RanksConfig ranksConfig;
    private final SellConfig sellConfig;

    public SellCommand(EconomyManager economyManager, RankService rankService, RanksConfig ranksConfig, SellConfig sellConfig) {
        super("sell", "sell the item in your hand");
        this.economyManager = economyManager;
        this.rankService = rankService;
        this.ranksConfig = ranksConfig;
        this.sellConfig = sellConfig;
        requirePermission("foxprison.sell.command.sell");
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
            playerRef.sendMessage(Message.translation("foxPrison.sell.command.sell.disabled"));
            return CompletableFuture.completedFuture(null);
        }

        // Get the player's prestige first
        return rankService.getPrestige(playerRef.getUuid())
                .thenAcceptAsync(prestige -> {

                    world.execute(() -> {
                        try {
                            Player player = store.getComponent(ref, Player.getComponentType());
                            if (player == null) {
                                playerRef.sendMessage(Message.translation("You're not in a world."));
                                return;
                            }

                            Inventory inv = player.getInventory();
                            ItemStack inHand = inv.getItemInHand();

                            if (inHand == null || inHand.isEmpty()) {
                                playerRef.sendMessage(Message.translation("foxPrison.sell.command.sell.no_item_in_hand"));
                                return;
                            }

                            String itemId = inHand.getItemId();
                            int qty = inHand.getQuantity();

                            double multiplier = 1.0 + ranksConfig.getPrestigeMultiplier(prestige);

                            SellPriceDefinition price = sellConfig.getPriceForItemId(itemId);
                            if (price == null || !price.isAllowSell() || price.getPriceEach() <= 0.0) {
                                playerRef.sendMessage(Message.translation("foxPrison.sell.command.sell.item_not_sellable")
                                        .param("item_id", itemId));
                                return;
                            }

                            double total = price.getPriceEach() * qty * multiplier;
                            if (total <= 0.0) {
                                playerRef.sendMessage(Message.translation("foxPrison.sell.command.sell.fail")
                                        .param("item_id", itemId));
                                return;
                            }

                            // Determine which container
                            final boolean usingToolsItem = inv.usingToolsItem();
                            final ItemContainer container = usingToolsItem ? inv.getTools() : inv.getHotbar();
                            final short slot = usingToolsItem ? inv.getActiveToolsSlot() : inv.getActiveHotbarSlot();

                            ItemStackSlotTransaction tx = container.setItemStackForSlot(slot, ItemStack.EMPTY);
                            if (!tx.succeeded()) {
                                playerRef.sendMessage(Message.translation("foxPrison.sell.command.sell.fail")
                                        .param("item_id", itemId));
                                return;
                            }

                            UUID uuid = playerRef.getUuid();
                            boolean depositOk = economyManager.deposit(uuid, total, "sellhand", price.getCurrency());

                            if (!depositOk) {
                                // refund item
                                container.setItemStackForSlot(slot, inHand);
                                playerRef.sendMessage(Message.translation("foxPrison.sell.command.sell.fail")
                                        .param("item_id", itemId));
                                return;
                            }

                            String totalFormatted = economyManager.format(total, price.getCurrency());
                            playerRef.sendMessage(Message.translation("foxPrison.sell.command.sell.success")
                                    .param("quantity", qty)
                                    .param("item_id", itemId)
                                    .param("currency_id", price.getCurrency())
                                    .param("currency", economyManager.getCurrencySingular(price.getCurrency()))
                                    .param("total", totalFormatted));

                        } catch (Throwable t) {
                            playerRef.sendMessage(Message.translation("foxPrison.sell.command.sell.fail")
                                    .param("item_id", "unknown"));
                            FoxPrisonPlugin.getInstance().getLogger().atSevere().log("Sell command error", t);
                        }
                    });

                });

    }
}

