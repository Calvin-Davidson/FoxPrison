package nl.trifox.foxprison.modules.economy;

import net.milkbowl.vault2.economy.AccountPermission;
import net.milkbowl.vault2.economy.Economy;
import net.milkbowl.vault2.economy.EconomyResponse;
import nl.trifox.foxprison.FoxPrisonPlugin;
import nl.trifox.foxprison.api.FoxPrisonAPI;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.*;

@SuppressWarnings("DeprecatedIsStillUsed")
public class VaultUnlockedEconomy implements Economy {

    private final FoxPrisonPlugin plugin = FoxPrisonPlugin.getInstance();
    private final String defaultCurrency = FoxPrisonPlugin.getInstance().getEconomyConfig().get().getDefaultCurrency();

    @Override
    public boolean isEnabled() {
        return plugin.getEconomyConfig().get().isEnabled();
    }

    @NotNull
    @Override
    public String getName() {

        return "EcoTale";
    }

    @Override
    public boolean hasSharedAccountSupport() {

        return false;
    }

    @Override
    public boolean hasMultiCurrencySupport() {

        return false;
    }

    @Override
    public int fractionalDigits(@NotNull final String pluginName) {

        return 0;
    }

    /**
     * @deprecated
     */
    @NotNull
    @Override
    public String format(@NotNull final BigDecimal amount) {

        return format(plugin.getName(), amount);
    }

    @NotNull
    @Override
    public String format(@NotNull final String pluginName, @NotNull final BigDecimal amount) {
        return amount.toString(); //Main.CONFIG.get().format(amount.doubleValue());
    }

    /**
     * @deprecated
     */
    @NotNull
    @Override
    public String format(@NotNull final BigDecimal amount, @NotNull final String currency) {
        return amount.toString(); // Main.CONFIG.get().format(amount.doubleValue());
    }

    @NotNull
    @Override
    public String format(@NotNull final String pluginName, @NotNull final BigDecimal amount, @NotNull final String currency) {
        return amount.toString(); // Main.CONFIG.get().format(amount.doubleValue());
    }

    @Override
    public boolean hasCurrency(@NotNull final String currency) {
        return false;
    }

    @NotNull
    @Override
    public String getDefaultCurrency(@NotNull final String pluginName) {
        return defaultCurrency;
    }

    @NotNull
    @Override
    public String defaultCurrencyNamePlural(@NotNull final String pluginName) {
        return defaultCurrency;
    }

    @NotNull
    @Override
    public String defaultCurrencyNameSingular(@NotNull final String pluginName) {
        return defaultCurrency;
    }

    @Override
    public @NotNull Collection<String> currencies() {

        //EcoTale doesn't support multi-currency
        return List.of(defaultCurrency);
    }

    /**
     * @deprecated
     */
    @Override
    public boolean createAccount(@NotNull final UUID accountID, @NotNull final String name) {

        return createAccount(accountID, name, true);
    }

    @Override
    public boolean createAccount(@NotNull final UUID accountID, @NotNull final String name, final boolean player) {

        if (!player) {
            //EcoTale doesn't support non-player accounts
            return false;
        }

        FoxPrisonPlugin.getEconomyModule().getEconomyManager().ensureAccount(accountID);
        return true;
    }

    /**
     * @deprecated
     */
    @Override
    public boolean createAccount(@NotNull final UUID accountID, @NotNull final String name, @NotNull final String worldName) {

        //refer back to the standard method because there's no concept of multi-world in EcoTale.
        return createAccount(accountID, name, true);
    }

    @Override
    public boolean createAccount(@NotNull final UUID accountID, @NotNull final String name, @NotNull final String worldName, final boolean player) {

        //refer back to the standard method because there's no concept of multi-world in EcoTale.
        return createAccount(accountID, name, player);
    }

    @Override
    public @NotNull Map<UUID, String> getUUIDNameMap() {

        //EcoTale doesn't store usernames
        return Map.of();
    }

    @Override
    public Optional<String> getAccountName(@NotNull final UUID accountID) {

        //EcoTale doesn't store usernames
        return Optional.empty();
    }

    @Override
    public boolean hasAccount(@NotNull final UUID accountID) {
        return true; // not implemented
    }

    @Override
    public boolean hasAccount(@NotNull final UUID accountID, @NotNull final String worldName) {

        //refer back to the standard method because there's no concept of multi-world in EcoTale.
        return hasAccount(accountID);
    }

    @Override
    public boolean renameAccount(@NotNull final UUID accountID, @NotNull final String name) {

        //ecotale doesn't store usernames
        return true;
    }

    @Override
    public boolean renameAccount(@NotNull final String pluginName, @NotNull final UUID accountID, @NotNull final String name) {

        //ecotale doesn't store usernames
        return true;
    }

    @Override
    public boolean deleteAccount(@NotNull final String pluginName, @NotNull final UUID accountID) {
        // not implemented
        return true;
    }

    @Override
    public boolean accountSupportsCurrency(@NotNull final String pluginName, @NotNull final UUID accountID, @NotNull final String currency) {

        //return true, because we don't have a concept of multi-currency in EcoTale
        return true;
    }

    @Override
    public boolean accountSupportsCurrency(@NotNull final String pluginName, @NotNull final UUID accountID, @NotNull final String currency, @NotNull final String world) {

        //return true, because we don't have a concept of multi-currency in EcoTale
        return true;
    }

    /**
     * @deprecated
     */
    @NotNull
    @Override
    public BigDecimal getBalance(@NotNull final String pluginName, @NotNull final UUID accountID) {
        return FoxPrisonAPI.getBalance(accountID);
    }

    /**
     * @deprecated
     */
    @NotNull
    @Override
    public BigDecimal getBalance(@NotNull final String pluginName, @NotNull final UUID accountID, @NotNull final String world) {

        //refer back to the standard method because there's no concept of multi-currency or multi-world in EcoTale.
        return getBalance(pluginName, accountID);
    }

    /**
     * @deprecated
     */
    @NotNull
    @Override
    public BigDecimal getBalance(@NotNull final String pluginName, @NotNull final UUID accountID, @NotNull final String world, @NotNull final String currency) {

        //refer back to the standard method because there's no concept of multi-currency or multi-world in EcoTale.
        return getBalance(pluginName, accountID);
    }

    @Override
    public boolean has(@NotNull final String pluginName, @NotNull final UUID accountID, @NotNull final BigDecimal amount) {

        return FoxPrisonAPI.hasBalance(accountID, amount.doubleValue());
    }

    @Override
    public boolean has(@NotNull final String pluginName, @NotNull final UUID accountID, @NotNull final String worldName, @NotNull final BigDecimal amount) {

        //refer back to the standard method because there's no concept of multi-currency or multi-world in EcoTale.
        return has(pluginName, accountID, amount);
    }

    @Override
    public boolean has(@NotNull final String pluginName, @NotNull final UUID accountID, @NotNull final String worldName, @NotNull final String currency, @NotNull final BigDecimal amount) {

        //refer back to the standard method because there's no concept of multi-currency or multi-world in EcoTale.
        return has(pluginName, accountID, amount);
    }

    @Override
    public @NotNull EconomyResponse withdraw(@NotNull final String pluginName, @NotNull final UUID accountID, @NotNull final BigDecimal amount) {

        final boolean withdrawResult = FoxPrisonAPI.withdraw(accountID, amount.doubleValue(), "VaultUnlocked withdraw from plugin " + pluginName);
        final EconomyResponse.ResponseType status = (withdrawResult)? EconomyResponse.ResponseType.SUCCESS : EconomyResponse.ResponseType.FAILURE;

        return new EconomyResponse(amount, getBalance(pluginName, accountID), status, "Withdrawal result: " + status.name());
    }

    @NotNull
    @Override
    public EconomyResponse withdraw(@NotNull final String pluginName, @NotNull final UUID accountID, @NotNull final String worldName, @NotNull final BigDecimal amount) {

        //refer back to the standard method because there's no concept of multi-currency or multi-world in EcoTale.
        return withdraw(pluginName, accountID, amount);
    }

    @NotNull
    @Override
    public EconomyResponse withdraw(@NotNull final String pluginName, @NotNull final UUID accountID, @NotNull final String worldName, @NotNull final String currency, @NotNull final BigDecimal amount) {

        //refer back to the standard method because there's no concept of multi-currency or multi-world in EcoTale.
        return withdraw(pluginName, accountID, amount);
    }

    @NotNull
    @Override
    public EconomyResponse deposit(@NotNull final String pluginName, @NotNull final UUID accountID, @NotNull final BigDecimal amount) {

        final boolean depositResult = FoxPrisonAPI.deposit(accountID, amount.doubleValue(), "VaultUnlocked deposit from plugin " + pluginName);
        final EconomyResponse.ResponseType status = (depositResult)? EconomyResponse.ResponseType.SUCCESS : EconomyResponse.ResponseType.FAILURE;

        return new EconomyResponse(amount, getBalance(pluginName, accountID), status, "Deposit result: " + status.name());
    }

    @NotNull
    @Override
    public EconomyResponse deposit(@NotNull final String pluginName, @NotNull final UUID accountID, @NotNull final String worldName, @NotNull final BigDecimal amount) {

        //refer back to the standard method because there's no concept of multi-currency or multi-world in EcoTale.
        return deposit(pluginName, accountID, amount);
    }

    @NotNull
    @Override
    public EconomyResponse deposit(@NotNull final String pluginName, @NotNull final UUID accountID, @NotNull final String worldName, @NotNull final String currency, @NotNull final BigDecimal amount) {

        //refer back to the standard method because there's no concept of multi-currency or multi-world in EcoTale.
        return deposit(pluginName, accountID, amount);
    }

    //The methods below don't need implemented because EcoTale doesn't support shared accounts.
    @Override
    public boolean createSharedAccount(@NotNull final String pluginName, @NotNull final UUID accountID, @NotNull final String name, @NotNull final UUID owner) {

        return false;
    }

    @Override
    public boolean isAccountOwner(@NotNull final String pluginName, @NotNull final UUID accountID, @NotNull final UUID uuid) {

        return false;
    }

    @Override
    public boolean setOwner(@NotNull final String pluginName, @NotNull final UUID accountID, @NotNull final UUID uuid) {

        return false;
    }

    @Override
    public boolean isAccountMember(@NotNull final String pluginName, @NotNull final UUID accountID, @NotNull final UUID uuid) {

        return false;
    }

    @Override
    public boolean addAccountMember(@NotNull final String pluginName, @NotNull final UUID accountID, @NotNull final UUID uuid) {

        return false;
    }

    @Override
    public boolean addAccountMember(@NotNull final String pluginName, @NotNull final UUID accountID, @NotNull final UUID uuid, final @NotNull AccountPermission... initialPermissions) {

        return false;
    }

    @Override
    public boolean removeAccountMember(@NotNull final String pluginName, @NotNull final UUID accountID, @NotNull final UUID uuid) {

        return false;
    }

    @Override
    public boolean hasAccountPermission(@NotNull final String pluginName, @NotNull final UUID accountID, @NotNull final UUID uuid, @NotNull final AccountPermission permission) {

        return false;
    }

    @Override
    public boolean updateAccountPermission(@NotNull final String pluginName, @NotNull final UUID accountID, @NotNull final UUID uuid, @NotNull final AccountPermission permission, final boolean value) {

        return false;
    }
}
