package nl.trifox.foxprison.modules.economy.command.admin;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import nl.trifox.foxprison.FoxPrisonPlugin;
import nl.trifox.foxprison.modules.economy.EconomyManager;
import nl.trifox.foxprison.modules.economy.config.CurrencyDefinition;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.concurrent.CompletableFuture;

public abstract class AbstractAsyncEconomyAdminCommand extends AbstractAsyncCommand {

    private final OptionalArg<PlayerRef> targetPlayerArg;
    private final OptionalArg<String> currencyIdArg;
    private final EconomyManager economyManager;

    public AbstractAsyncEconomyAdminCommand(@NotNull String name, @NotNull String description) {
        super(name, description);

        this.currencyIdArg = this.withOptionalArg("currencyId", "the currency you want to set", ArgTypes.STRING);
        this.targetPlayerArg = this.withOptionalArg("targetPlayer", "which player you want to set", ArgTypes.PLAYER_REF);
        this.economyManager = FoxPrisonPlugin.getEconomyModule().getEconomyManager();

    }

    @Override
    protected final @NotNull CompletableFuture<Void> executeAsync(@NotNull CommandContext ctx) {
        var currencyID = currencyIdArg.provided(ctx) ? currencyIdArg.get(ctx) : economyManager.getDefaultCurrencyID();

        CommandSender sender = ctx.sender();
        if (!(sender instanceof Player) && !targetPlayerArg.provided(ctx)) {
            ctx.sendMessage(Message.raw("Cannot target self as you are not a player").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        var targetRef = targetPlayerArg.provided(ctx) ? targetPlayerArg.get(ctx).getReference() : ctx.senderAsPlayerRef();
        if (targetRef == null) {
            sender.sendMessage(Message.raw("invalid player target"));
            return CompletableFuture.completedFuture(null);
        }
        var config = FoxPrisonPlugin.getInstance().getEconomyConfig();

        return executeAsync(ctx, targetRef, config.get().getCurrency(currencyID));
    }

    protected abstract CompletableFuture<Void> executeAsync(@NotNull CommandContext context, Ref<EntityStore> targetRef, CurrencyDefinition currency);

    public EconomyManager getEconomyManager() {
        return economyManager;
    }
}
