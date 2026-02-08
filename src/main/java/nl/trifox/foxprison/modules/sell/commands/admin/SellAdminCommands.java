package nl.trifox.foxprison.modules.sell.commands.admin;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.FlagArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import nl.trifox.foxprison.FoxPrisonPlugin;
import nl.trifox.foxprison.modules.economy.EconomyManager;
import nl.trifox.foxprison.modules.sell.config.SellConfig;
import nl.trifox.foxprison.modules.sell.config.SellPriceDefinition;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Wire this up in setup():
 *   getCommandRegistry().registerCommand(new SellAdminCommands(sellManager));
 */
public final class SellAdminCommands extends AbstractCommandCollection {

    public SellAdminCommands(@Nonnull SellConfig config, @Nonnull EconomyManager economyManager) {
        super("selladmin", "Admin: manage /sell prices");
        requirePermission("foxprison.sell.command.selladmin");
        addAliases("sa");

        addSubCommand(new SetPriceSub(config, economyManager));
        addSubCommand(new SumSub(config, economyManager));
    }


    private static final class SetPriceSub extends AbstractAsyncCommand {

        private final SellConfig sell;
        private final EconomyManager economyManager;

        private final RequiredArg<Double> priceArg;
        private final OptionalArg<String> itemArg;
        private final OptionalArg<String> currencyArg;
        private final FlagArg noSaveFlag;

        private SetPriceSub(SellConfig sell, EconomyManager economyManager) {
            super("setprice", "Set sell price for an item (defaults to item in hand)");
            this.sell = Objects.requireNonNull(sell);
            this.economyManager = economyManager;

            requirePermission("foxprison.sell.admin.setprice");

            priceArg = withRequiredArg("price", "Price per item", ArgTypes.DOUBLE);
            itemArg = withOptionalArg("item", "ItemId (if omitted, uses item in hand)", ArgTypes.STRING);
            currencyArg = withOptionalArg("currency", "Currency id (defaults to global default)", ArgTypes.STRING);
            noSaveFlag = withFlagArg("nosave", "Do not persist to disk");
        }

        @Override
        @Nonnull
        protected CompletableFuture<Void> executeAsync(@Nonnull CommandContext context) {
            String itemId = itemArg.provided(context) ? safeTrim(itemArg.get(context)) : null;

            if (itemId == null || itemId.isBlank()) {
                itemId = getHeldItemIdOrNull(context);
                if (itemId == null) {
                    context.sender().sendMessage(Message.raw("Hold an item, or pass --item <ItemId>."));
                    return CompletableFuture.completedFuture(null);
                }
            }

            double price = priceArg.get(context);
            if (price < 0.0) {
                context.sender().sendMessage(Message.raw("Price must be >= 0."));
                return CompletableFuture.completedFuture(null);
            }

            String currency = currencyArg.provided(context) ? safeTrim(currencyArg.get(context)) : economyManager.getDefaultCurrencyID();
            if (currency == null || currency.isBlank()) currency = economyManager.getDefaultCurrencyID();

            SellPriceDefinition def = new SellPriceDefinition(price, currency);
            sell.setPrice(itemId, def);

            CompletableFuture<Void> saveFuture = noSaveFlag.get(context)
                    ? CompletableFuture.completedFuture(null)
                    : FoxPrisonPlugin.getInstance().getSellConfig().save();

            String finalItemId = itemId;
            String finalCurrency = currency;
            return saveFuture.handle((ok, err) -> {
                if (err != null) {
                    context.sender().sendMessage(Message.raw(
                            "Set price for " + finalItemId + " to " + price + " (" + finalCurrency + "), but saving failed: " + err.getMessage()
                    ));
                } else {
                    context.sender().sendMessage(Message.raw(
                            "Set price for " + finalItemId + " to " + price + " (" + finalCurrency + ")."
                    ));
                }
                return null;
            });
        }
    }

    private static final class SumSub extends AbstractAsyncCommand {

        private final SellConfig sell;
        private final OptionalArg<String> scopeArg;

        private SumSub(SellConfig sell, EconomyManager economyManager) {
            super("sum", "Show total sell value (hand or inventory)");
            this.sell = Objects.requireNonNull(sell);


            requirePermission("foxprison.sell.admin.sum");

            scopeArg = withOptionalArg("scope", "hand|inventory (default: inventory)", ArgTypes.STRING);
        }

        @Override
        @Nonnull
        protected CompletableFuture<Void> executeAsync(@Nonnull CommandContext context) {
            if (!(context.sender() instanceof Player player)) {
                context.sender().sendMessage(Message.raw("This command is player-only."));
                return CompletableFuture.completedFuture(null);
            }

            String scope = scopeArg.provided(context) ? safeTrim(scopeArg.get(context)) : "inventory";
            if (scope == null || scope.isBlank()) scope = "inventory";
            scope = scope.toLowerCase();

            Inventory inv = player.getInventory();
            double total = 0.0;
            int pricedStacks = 0;
            int skippedStacks = 0;

            if (scope.equals("hand")) {
                ItemStack held = inv.getItemInHand();
                if (held == null || held.isEmpty()) {
                    player.sendMessage(Message.raw("You're not holding anything."));
                    return CompletableFuture.completedFuture(null);
                }

                SellPriceDefinition def = sell.getPriceForItemId(held.getItemId());
                if (def == null || def.getPriceEach() <= 0.0) {
                    player.sendMessage(Message.raw("No sell price set for " + held.getItemId() + "."));
                    return CompletableFuture.completedFuture(null);
                }

                total = def.getPriceEach() * held.getQuantity();
                player.sendMessage(Message.raw(
                        "Hand value: " + total + " (" + def.getCurrency() + ") for "
                                + held.getQuantity() + "x " + held.getItemId()
                ));
                return CompletableFuture.completedFuture(null);
            }

            CombinedItemContainer everything = inv.getCombinedEverything();
            final short cap = everything.getCapacity();
            for (short slot = 0; slot < cap; slot++) {
                ItemStack stack = everything.getItemStack(slot);
                if (stack == null || stack.isEmpty()) continue;

                SellPriceDefinition def = sell.getPriceForItemId(stack.getItemId());
                if (def == null || def.getPriceEach() <= 0.0) {
                    skippedStacks++;
                    continue;
                }

                total += def.getPriceEach() * stack.getQuantity();
                pricedStacks++;
            }

            player.sendMessage(Message.raw(
                    "Inventory value: " + total + " (priced stacks: " + pricedStacks + ", skipped: " + skippedStacks + ")."
            ));

            return CompletableFuture.completedFuture(null);
        }
    }

    private static String getHeldItemIdOrNull(CommandContext context) {
        if (!(context.sender() instanceof Player player)) return null;

        ItemStack held = player.getInventory().getItemInHand();
        if (held == null || held.isEmpty()) return null;

        String id = held.getItemId();
        return id.isBlank() ? null : id.trim();
    }

    private static String safeTrim(String s) {
        return s == null ? null : s.trim();
    }
}