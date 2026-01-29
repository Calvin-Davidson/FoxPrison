package nl.trifox.foxprison.modules.economy.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import nl.trifox.foxprison.modules.economy.config.CurrencyDefinition;
import nl.trifox.foxprison.modules.economy.config.EconomyConfig;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

public class PlayerBalanceData {

    public static final BuilderCodec<PlayerBalanceData> CODEC =
            BuilderCodec.builder(PlayerBalanceData.class, PlayerBalanceData::new)
                    .append(new KeyedCodec<>("Uuid", Codec.STRING),
                            (p, v, i) -> p.playerUuid = UUID.fromString(v),
                            (p, i) -> p.playerUuid.toString())
                    .add()
                    .append(new KeyedCodec<>("Wallets", CurrencyWallet.ARRAY_CODEC),
                            (p, v, i) -> p.wallets = (v == null ? new CurrencyWallet[0] : v),
                            (p, i) -> p.wallets)
                    .add()
                    .build();

    private UUID playerUuid;
    private CurrencyWallet[] wallets = new CurrencyWallet[0];

    public PlayerBalanceData() {}

    public PlayerBalanceData(UUID playerUuid) {
        this.playerUuid = playerUuid;
    }

    public UUID getPlayerUuid() { return playerUuid; }

    public CurrencyWallet[] getWallets() {
        return wallets == null ? new CurrencyWallet[0] : wallets;
    }

    public void setPlayerUuidIfMissing(UUID uuid) {
        if (this.playerUuid == null) this.playerUuid = uuid;
    }

    public void ensureWallets(EconomyConfig cfg) {
        Objects.requireNonNull(cfg, "cfg");

        for (String id : cfg.getCurrencyIds()) {
            CurrencyWallet w = findWallet(id);
            if (w == null) {
                CurrencyWallet created = new CurrencyWallet(id);
                created.setBalance(cfg.getCurrency(id).getDecimalPlaces(), "Starting balance");
                addWallet(created);
            }
        }
    }

    public double getBalance(String currencyId) {
        CurrencyWallet w = wallet(currencyId);
        return w.getBalance();
    }

    public boolean hasBalance(String currencyId, double amount) {
        return wallet(currencyId).hasBalance(amount);
    }

    public boolean deposit(String currencyId, double amount, String reason) {
        return wallet(currencyId).deposit(amount, reason);
    }

    public boolean withdraw(String currencyId, double amount, String reason) {
        return wallet(currencyId).withdraw(amount, reason);
    }

    public void depositUnsafe(String currencyId, double amount, String reason) {
        wallet(currencyId).depositUnsafe(amount, reason);
    }

    public void withdrawUnsafe(String currencyId, double amount, String reason) {
         wallet(currencyId).withdrawUnsafe(amount, reason);
    }


    public void setBalance(String currencyId, double amount, String reason) {
        wallet(currencyId).setBalance(amount, reason);
    }

    // ===== Internals =====

    private CurrencyWallet wallet(String currencyId) {
        CurrencyWallet existing = findWallet(currencyId);
        if (existing != null) return existing;

        CurrencyWallet created = new CurrencyWallet(currencyId);
        addWallet(created);
        return created;
    }

    private CurrencyWallet findWallet(String currencyId) {
        if (wallets == null || wallets.length == 0) return null;
        for (CurrencyWallet w : wallets) {
            if (w != null && CurrencyDefinition.normalize(w.getCurrencyId()).equals(currencyId)) {
                return w;
            }
        }
        return null;
    }

    private void addWallet(CurrencyWallet w) {
        if (wallets == null) wallets = new CurrencyWallet[0];
        CurrencyWallet[] newArr = Arrays.copyOf(wallets, wallets.length + 1);
        newArr[newArr.length - 1] = w;
        wallets = newArr;
    }

    public static final class CurrencyWallet {

        public static final BuilderCodec<CurrencyWallet> CODEC =
                BuilderCodec.builder(CurrencyWallet.class, CurrencyWallet::new)
                        .append(new KeyedCodec<>("CurrencyId", Codec.STRING),
                                (w, v, i) -> w.currencyId = CurrencyDefinition.normalize(v),
                                (w, i) -> w.currencyId)
                        .add()
                        .append(new KeyedCodec<>("Balance", Codec.DOUBLE),
                                (w, v, i) -> w.balance = v,
                                (w, i) -> w.balance)
                        .add()
                        .append(new KeyedCodec<>("TotalEarned", Codec.DOUBLE),
                                (w, v, i) -> w.totalEarned = v,
                                (w, i) -> w.totalEarned)
                        .add()
                        .append(new KeyedCodec<>("TotalSpent", Codec.DOUBLE),
                                (w, v, i) -> w.totalSpent = v,
                                (w, i) -> w.totalSpent)
                        .add()
                        .append(new KeyedCodec<>("LastTransaction", Codec.STRING),
                                (w, v, i) -> w.lastTransaction = v,
                                (w, i) -> w.lastTransaction)
                        .add()
                        .append(new KeyedCodec<>("LastTransactionTime", Codec.LONG),
                                (w, v, i) -> w.lastTransactionTime = v,
                                (w, i) -> w.lastTransactionTime)
                        .add()
                        .build();

        public static final Codec<CurrencyWallet[]> ARRAY_CODEC =
                new ArrayCodec<>(CODEC, CurrencyWallet[]::new, CurrencyWallet::new);

        private String currencyId = "money";
        private double balance = 0;
        private double totalEarned = 0;
        private double totalSpent = 0;
        private String lastTransaction = "";
        private long lastTransactionTime = 0;

        public CurrencyWallet() {}

        public CurrencyWallet(String currencyId) {
            this.currencyId = CurrencyDefinition.normalize(currencyId);
        }

        public String getCurrencyId() { return currencyId; }
        public double getBalance() { return balance; }
        public double getTotalEarned() { return totalEarned; }
        public double getTotalSpent() { return totalSpent; }
        public String getLastTransaction() { return lastTransaction; }
        public long getLastTransactionTime() { return lastTransactionTime; }

        public boolean hasBalance(double amount) { return balance >= amount; }

        public boolean deposit(double amount, String reason) {
            if (amount <= 0) return false;
            balance += amount;
            totalEarned += amount;
            lastTransaction = "+" + amount + " (" + reason + ")";
            lastTransactionTime = System.currentTimeMillis();
            return true;
        }

        public boolean withdraw(double amount, String reason) {
            if (amount <= 0 || balance < amount) return false;
            balance -= amount;
            totalSpent += amount;
            lastTransaction = "-" + amount + " (" + reason + ")";
            lastTransactionTime = System.currentTimeMillis();
            return true;
        }

        public void setBalance(double amount, String reason) {
            balance = Math.max(0, amount);
            lastTransaction = "Set to " + amount + " (" + reason + ")";
            lastTransactionTime = System.currentTimeMillis();
        }

        // Unsafe variants if you need them later
        public void depositUnsafe(double amount, String reason) {
            balance += amount;
            totalEarned += amount;
            lastTransaction = "+" + amount + " (" + reason + ")";
            lastTransactionTime = System.currentTimeMillis();
        }

        public void withdrawUnsafe(double amount, String reason) {
            balance -= amount;
            totalSpent += amount;
            lastTransaction = "-" + amount + " (" + reason + ")";
            lastTransactionTime = System.currentTimeMillis();
        }
    }
}
