package com.aegisguard.coffers.legacy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import net.milkbowl.vault.economy.AbstractEconomy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

final class CoffersLegacyVaultEconomy extends AbstractEconomy {

    private final CoffersLegacyPlugin plugin;
    private final CoffersLegacyEconomyService economy;

    CoffersLegacyVaultEconomy(final CoffersLegacyPlugin plugin, final CoffersLegacyEconomyService economy) {
        this.plugin = plugin;
        this.economy = economy;
    }

    public boolean isEnabled() {
        return this.plugin.isEnabled();
    }

    public String getName() {
        return "CoffersLegacy";
    }

    public boolean hasBankSupport() {
        return false;
    }

    public int fractionalDigits() {
        return this.economy.currency(this.economy.getDefaultCurrencyId()).getFractionalDigits();
    }

    public String format(final double amount) {
        return this.economy.format(this.economy.getDefaultCurrencyId(), BigDecimal.valueOf(amount));
    }

    public String currencyNamePlural() {
        return this.economy.currency(this.economy.getDefaultCurrencyId()).getPluralName();
    }

    public String currencyNameSingular() {
        return this.economy.currency(this.economy.getDefaultCurrencyId()).getSingularName();
    }

    public boolean hasAccount(final String playerName) {
        return hasAccount(Bukkit.getOfflinePlayer(playerName));
    }

    public boolean hasAccount(final OfflinePlayer player) {
        return this.economy.hasAccount(player.getUniqueId());
    }

    public boolean hasAccount(final String playerName, final String worldName) {
        return hasAccount(playerName);
    }

    public boolean hasAccount(final OfflinePlayer player, final String worldName) {
        return hasAccount(player);
    }

    public double getBalance(final String playerName) {
        return getBalance(Bukkit.getOfflinePlayer(playerName));
    }

    public double getBalance(final OfflinePlayer player) {
        return this.economy.getBalance(player.getUniqueId(), this.economy.getDefaultCurrencyId()).doubleValue();
    }

    public double getBalance(final String playerName, final String world) {
        return getBalance(playerName);
    }

    public double getBalance(final OfflinePlayer player, final String world) {
        return getBalance(player);
    }

    public boolean has(final String playerName, final double amount) {
        return getBalance(playerName) >= amount;
    }

    public boolean has(final OfflinePlayer player, final double amount) {
        return getBalance(player) >= amount;
    }

    public boolean has(final String playerName, final String worldName, final double amount) {
        return has(playerName, amount);
    }

    public boolean has(final OfflinePlayer player, final String worldName, final double amount) {
        return has(player, amount);
    }

    public EconomyResponse withdrawPlayer(final String playerName, final double amount) {
        return withdrawPlayer(Bukkit.getOfflinePlayer(playerName), amount);
    }

    public EconomyResponse withdrawPlayer(final OfflinePlayer player, final double amount) {
        return response(this.economy.withdraw(player.getUniqueId(), this.economy.getDefaultCurrencyId(), BigDecimal.valueOf(amount), LegacyTransactionActor.vault("vault-withdraw"), "Vault withdraw"));
    }

    public EconomyResponse withdrawPlayer(final String playerName, final String worldName, final double amount) {
        return withdrawPlayer(playerName, amount);
    }

    public EconomyResponse withdrawPlayer(final OfflinePlayer player, final String worldName, final double amount) {
        return withdrawPlayer(player, amount);
    }

    public EconomyResponse depositPlayer(final String playerName, final double amount) {
        return depositPlayer(Bukkit.getOfflinePlayer(playerName), amount);
    }

    public EconomyResponse depositPlayer(final OfflinePlayer player, final double amount) {
        return response(this.economy.deposit(player.getUniqueId(), this.economy.getDefaultCurrencyId(), BigDecimal.valueOf(amount), LegacyTransactionActor.vault("vault-deposit"), "Vault deposit"));
    }

    public EconomyResponse depositPlayer(final String playerName, final String worldName, final double amount) {
        return depositPlayer(playerName, amount);
    }

    public EconomyResponse depositPlayer(final OfflinePlayer player, final String worldName, final double amount) {
        return depositPlayer(player, amount);
    }

    public EconomyResponse createBank(final String name, final String player) {
        return unsupportedBanks();
    }

    public EconomyResponse createBank(final String name, final OfflinePlayer player) {
        return unsupportedBanks();
    }

    public EconomyResponse deleteBank(final String name) {
        return unsupportedBanks();
    }

    public EconomyResponse bankBalance(final String name) {
        return unsupportedBanks();
    }

    public EconomyResponse bankHas(final String name, final double amount) {
        return unsupportedBanks();
    }

    public EconomyResponse bankWithdraw(final String name, final double amount) {
        return unsupportedBanks();
    }

    public EconomyResponse bankDeposit(final String name, final double amount) {
        return unsupportedBanks();
    }

    public EconomyResponse isBankOwner(final String name, final String playerName) {
        return unsupportedBanks();
    }

    public EconomyResponse isBankOwner(final String name, final OfflinePlayer player) {
        return unsupportedBanks();
    }

    public EconomyResponse isBankMember(final String name, final String playerName) {
        return unsupportedBanks();
    }

    public EconomyResponse isBankMember(final String name, final OfflinePlayer player) {
        return unsupportedBanks();
    }

    public List<String> getBanks() {
        return new ArrayList<String>();
    }

    public boolean createPlayerAccount(final String playerName) {
        return createPlayerAccount(Bukkit.getOfflinePlayer(playerName));
    }

    public boolean createPlayerAccount(final OfflinePlayer player) {
        this.economy.createAccount(player.getUniqueId());
        return true;
    }

    public boolean createPlayerAccount(final String playerName, final String worldName) {
        return createPlayerAccount(playerName);
    }

    public boolean createPlayerAccount(final OfflinePlayer player, final String worldName) {
        return createPlayerAccount(player);
    }

    private EconomyResponse response(final LegacyTransactionResult result) {
        EconomyResponse.ResponseType type = result.isSuccessful()
                ? EconomyResponse.ResponseType.SUCCESS
                : EconomyResponse.ResponseType.FAILURE;
        return new EconomyResponse(result.getAmount().doubleValue(), result.getBalance().doubleValue(), type, result.getMessage());
    }

    private EconomyResponse unsupportedBanks() {
        return new EconomyResponse(0.0D, 0.0D, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Coffers Legacy bank accounts are not implemented yet.");
    }
}
