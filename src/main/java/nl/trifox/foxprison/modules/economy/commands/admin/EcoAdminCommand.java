package nl.trifox.foxprison.modules.economy.commands.admin;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import nl.trifox.foxprison.FoxPrisonPlugin;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.*;
import java.util.concurrent.CompletableFuture;

public class EcoAdminCommand extends AbstractAsyncCommand {

    public EcoAdminCommand() {
        super("eco", "Economy administration commands");
        this.addAliases("economy", "ecoadmin");
        this.setPermissionGroup(null); // Admin only - requires ecotale.ecotale.command.eco permission

        this.addSubCommand(new EcoSetCommand());
        this.addSubCommand(new EcoGiveCommand());
        this.addSubCommand(new EcoTakeCommand());
        this.addSubCommand(new EcoResetCommand());
        this.addSubCommand(new EcoSaveCommand());
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext commandContext) {
        CommandSender sender = commandContext.sender();

        // Fallback: show help text
        commandContext.sender().sendMessage(Message.raw("=== Ecotale Economy Admin ===").color(new Color(255, 215, 0)));
        commandContext.sender().sendMessage(Message.raw("  /eco set <amount> - Set your balance").color(Color.GRAY));
        commandContext.sender().sendMessage(Message.raw("  /eco give <amount> - Add to balance").color(Color.GRAY));
        commandContext.sender().sendMessage(Message.raw("  /eco take <amount> - Remove from balance").color(Color.GRAY));
        commandContext.sender().sendMessage(Message.raw("  /eco reset - Reset to starting balance").color(Color.GRAY));
        commandContext.sender().sendMessage(Message.raw("  /eco save - Force save data").color(Color.GRAY));
        return CompletableFuture.completedFuture(null);
    }

    // ========== SET COMMAND ==========
    private static class EcoSetCommand extends AbstractAsyncCommand {
        private final RequiredArg<Double> amountArg;

        public EcoSetCommand() {
            super("set", "Set your balance to a specific amount");
            this.amountArg = this.withRequiredArg("amount", "The amount to set", ArgTypes.DOUBLE);
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            CommandSender sender = ctx.sender();
            if (!(sender instanceof Player player)) {
                ctx.sendMessage(Message.raw("This command can only be used by players").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            Double amount = amountArg.get(ctx);
            if (amount == null || amount < 0) {
                ctx.sendMessage(Message.raw("Amount must be positive").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            var ref = player.getReference();
            if (ref == null || !ref.isValid()) return CompletableFuture.completedFuture(null);

            var store = ref.getStore();
            var world = store.getExternalData().getWorld();

            return CompletableFuture.runAsync(() -> {
                PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
                if (playerRef == null) return;

                double oldBalance = FoxPrisonPlugin.getEconomyModule().getEconomyManager().getBalance(playerRef.getUuid());
                FoxPrisonPlugin.getEconomyModule().getEconomyManager().setBalance(playerRef.getUuid(), amount, "Admin set");

                player.sendMessage(Message.join(
                        Message.raw("Balance set: ").color(Color.GREEN),
                        Message.raw(FoxPrisonPlugin.getInstance().getCoreConfig().get().format(oldBalance)).color(Color.GRAY),
                        Message.raw(" -> ").color(Color.WHITE),
                        Message.raw(FoxPrisonPlugin.getInstance().getCoreConfig().get().format(amount)).color(new Color(50, 205, 50))
                ));
            }, world);
        }
    }

    // ========== GIVE COMMAND ==========
    private static class EcoGiveCommand extends AbstractAsyncCommand {
        private final RequiredArg<Double> amountArg;

        public EcoGiveCommand() {
            super("give", "Add money to your balance");
            this.addAliases("add");
            this.amountArg = this.withRequiredArg("amount", "The amount to add", ArgTypes.DOUBLE);
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            CommandSender sender = ctx.sender();
            if (!(sender instanceof Player player)) {
                ctx.sendMessage(Message.raw("This command can only be used by players").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            Double amount = amountArg.get(ctx);
            if (amount == null || amount <= 0) {
                ctx.sendMessage(Message.raw("Amount must be positive").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            var ref = player.getReference();
            if (ref == null || !ref.isValid()) {
                return CompletableFuture.completedFuture(null);
            }

            var store = ref.getStore();
            var world = store.getExternalData().getWorld();

            // Run on world thread to avoid threading issues
            return CompletableFuture.runAsync(() -> {
                PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
                if (playerRef == null) return;

                FoxPrisonPlugin.getEconomyModule().getEconomyManager().deposit(playerRef.getUuid(), amount, "Admin give");
                double newBalance = FoxPrisonPlugin.getEconomyModule().getEconomyManager().getBalance(playerRef.getUuid());

                player.sendMessage(Message.join(
                        Message.raw("Added ").color(Color.GREEN),
                        Message.raw("+" + FoxPrisonPlugin.getInstance().getCoreConfig().get().format(amount)).color(new Color(50, 205, 50)),
                        Message.raw(" | New balance: ").color(Color.GRAY),
                        Message.raw(FoxPrisonPlugin.getInstance().getCoreConfig().get().format(newBalance)).color(Color.WHITE)
                ));
            }, world);
        }
    }

    // ========== TAKE COMMAND ==========
    private static class EcoTakeCommand extends AbstractAsyncCommand {
        private final RequiredArg<Double> amountArg;

        public EcoTakeCommand() {
            super("take", "Remove money from your balance");
            this.addAliases("remove");
            this.amountArg = this.withRequiredArg("amount", "The amount to remove", ArgTypes.DOUBLE);
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            CommandSender sender = ctx.sender();
            if (!(sender instanceof Player player)) {
                ctx.sendMessage(Message.raw("This command can only be used by players").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            Double amount = amountArg.get(ctx);
            if (amount == null || amount <= 0) {
                ctx.sendMessage(Message.raw("Amount must be positive").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            var ref = player.getReference();
            if (ref == null || !ref.isValid()) return CompletableFuture.completedFuture(null);

            var store = ref.getStore();
            var world = store.getExternalData().getWorld();

            return CompletableFuture.runAsync(() -> {
                PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
                if (playerRef == null) return;

                boolean success = FoxPrisonPlugin.getEconomyModule().getEconomyManager().withdraw(playerRef.getUuid(), amount, "Admin take");
                double newBalance = FoxPrisonPlugin.getEconomyModule().getEconomyManager().getBalance(playerRef.getUuid());

                if (success) {
                    player.sendMessage(Message.join(
                            Message.raw("Removed ").color(Color.YELLOW),
                            Message.raw("-" + FoxPrisonPlugin.getInstance().getCoreConfig().get().format(amount)).color(new Color(255, 99, 71)),
                            Message.raw(" | New balance: ").color(Color.GRAY),
                            Message.raw(FoxPrisonPlugin.getInstance().getCoreConfig().get().format(newBalance)).color(Color.WHITE)
                    ));
                } else {
                    player.sendMessage(Message.raw("Insufficient funds").color(Color.RED));
                }
            }, world);
        }
    }

    // ========== RESET COMMAND ==========
    private static class EcoResetCommand extends AbstractAsyncCommand {
        public EcoResetCommand() {
            super("reset", "Reset balance to starting amount");
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            CommandSender sender = ctx.sender();
            if (!(sender instanceof Player player)) {
                ctx.sendMessage(Message.raw("This command can only be used by players").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            var ref = player.getReference();
            if (ref == null || !ref.isValid()) return CompletableFuture.completedFuture(null);

            var store = ref.getStore();
            var world = store.getExternalData().getWorld();

            return CompletableFuture.runAsync(() -> {
                PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
                if (playerRef == null) return;

                double startingBalance = FoxPrisonPlugin.getInstance().getCoreConfig().get().getStartingBalance();
                FoxPrisonPlugin.getEconomyModule().getEconomyManager().setBalance(playerRef.getUuid(), startingBalance, "Admin reset");

                player.sendMessage(Message.join(
                        Message.raw("Balance reset to ").color(Color.GREEN),
                        Message.raw(String.valueOf(startingBalance)).color(new Color(50, 205, 50))
                ));
            }, world);
        }
    }


    // ========== SAVE COMMAND ==========
    private static class EcoSaveCommand extends AbstractAsyncCommand {
        public EcoSaveCommand() {
            super("save", "Force save all data");
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            FoxPrisonPlugin.getEconomyModule().getEconomyManager().forceSave();
            ctx.sendMessage(Message.raw("✓ Economy data saved successfully").color(Color.GREEN));
            return CompletableFuture.completedFuture(null);
        }
    }
}