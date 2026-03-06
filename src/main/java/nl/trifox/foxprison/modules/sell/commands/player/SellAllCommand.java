package nl.trifox.foxprison.modules.sell.commands.player;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackSlotTransaction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import nl.trifox.foxprison.modules.economy.EconomyManager;
import nl.trifox.foxprison.modules.ranks.RankService;
import nl.trifox.foxprison.modules.ranks.config.RanksConfig;
import nl.trifox.foxprison.modules.sell.config.SellAllDefinition;
import nl.trifox.foxprison.modules.sell.config.SellConfig;
import nl.trifox.foxprison.modules.sell.config.SellPriceDefinition;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class SellAllCommand extends AbstractAsyncPlayerCommand {

    private static final DecimalFormat MONEY_FMT = new DecimalFormat("#,##0.##");

    private final EconomyManager economyManager;
    private final RankService rankService;
    private final RanksConfig ranksConfig;
    private final SellConfig sellConfig;

    public SellAllCommand(EconomyManager economyManager, RankService rankService, RanksConfig ranksConfig, SellConfig sellConfig) {
        super("sellall", "auto sell your entire inventory");
        this.economyManager = economyManager;
        this.rankService = rankService;
        this.ranksConfig = ranksConfig;
        this.sellConfig = sellConfig;
        requirePermission("foxprison.sell.command.sellall");
    }

    private record SlotSell(ItemContainer container, short slot, ItemStack original, double value) {
    }

    @Override
    protected @NotNull CompletableFuture<Void> executeAsync(
            @NotNull CommandContext commandContext,
            @NotNull Store<EntityStore> store,
            @NotNull Ref<EntityStore> ref,
            @NotNull PlayerRef playerRef,
            @NotNull World world
    ) {
        if (!sellConfig.isEnabled() || !sellConfig.isSellAllEnabled()) {
            playerRef.sendMessage(Message.translation("foxPrison.sell.command.sellall.disabled"));
            return CompletableFuture.completedFuture(null);
        }


        return rankService.getPrestige(playerRef.getUuid())
                .thenAcceptAsync(prestige -> {
                    double multiplier = 1.0 + ranksConfig.getPrestigeMultiplier(prestige);

                    world.execute(() -> {
                        try {
                            Player player = store.getComponent(ref, Player.getComponentType());
                            if (player == null) {
                                playerRef.sendMessage(Message.raw("You're not in a world."));
                                return;
                            }

                            Inventory inv = player.getInventory();
                            SellAllDefinition s = sellConfig.getSellAll();

                            List<ItemContainer> containers = new ArrayList<>();
                            if (s.isIncludeHotbar()) containers.add(inv.getHotbar());
                            if (s.isIncludeMainInventory()) containers.add(inv.getStorage());
                            if (s.isIncludeEquipment()) containers.add(inv.getArmor());
                            if (s.isIncludeOffhand()) containers.add(inv.getUtility());

                            List<SlotSell> toSell = new ArrayList<>();
                            int stacksSold = 0;
                            AtomicInteger unsellableStacks = new AtomicInteger();
                            double total = 0.0;

                            for (ItemContainer container : containers) {
                                if (container == null) continue;

                                container.forEach((slot, stack) -> {
                                    if (stack == null || stack.isEmpty()) return;

                                    SellPriceDefinition price = sellConfig.getPriceForItemId(stack.getItemId());
                                    if (price == null || !price.isAllowSellAll() || price.getPriceEach() <= 0.0) {
                                        unsellableStacks.getAndIncrement();
                                        return;
                                    }

                                    double value = price.getPriceEach() * stack.getQuantity() * multiplier;
                                    if (value <= 0.0) {
                                        unsellableStacks.getAndIncrement();
                                        return;
                                    }

                                    toSell.add(new SlotSell(container, slot, stack, value));
                                });
                            }

                            for (SlotSell ss : toSell) {
                                total += ss.value;
                                stacksSold++;
                            }

                            if (toSell.isEmpty() || total <= 0.0) {
                                playerRef.sendMessage(Message.translation("foxPrison.sell.command.sellall.nothing_to_sell"));
                                 return;
                            }

                            // Remove first (anti-dupe)
                            List<SlotSell> removed = new ArrayList<>(toSell.size());
                            for (SlotSell ss : toSell) {
                                ItemStackSlotTransaction tx = ss.container.setItemStackForSlot(ss.slot, ItemStack.EMPTY);
                                if (!tx.succeeded()) {
                                    // rollback what we already removed
                                    for (SlotSell r : removed) {
                                        r.container.setItemStackForSlot(r.slot, r.original);
                                    }
                                    playerRef.sendMessage(Message.translation("foxPrison.sell.command.sellall.fail"));
                                     return;
                                }
                                removed.add(ss);
                            }

                            UUID uuid = playerRef.getUuid();
                            var depositOk = economyManager.deposit(uuid, total, "sellall");

                            if (!depositOk) {
                                world.execute(() -> {
                                    for (SlotSell r : removed) {
                                        r.container.setItemStackForSlot(r.slot, r.original);
                                    }
                                });
                                playerRef.sendMessage(Message.translation("foxPrison.sell.command.sellall.fail"));
                                return;
                            }

                            playerRef.sendMessage(Message.translation("foxPrison.sell.command.sellall.success").param("items_count", stacksSold).param("currency", "money").param("total", total));

                        } catch (Throwable t) {
                            // swallow
                        }
                    });
                });
    }
}
