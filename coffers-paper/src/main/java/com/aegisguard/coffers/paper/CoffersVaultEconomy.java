package com.aegisguard.coffers.paper;

import com.aegisguard.coffers.api.CoffersEconomy;
import com.aegisguard.coffers.api.TransactionActor;
import com.aegisguard.coffers.api.TransactionResult;
import java.math.BigDecimal;
import java.util.List;
import net.milkbowl.vault.economy.AbstractEconomy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

final class CoffersVaultEconomy extends AbstractEconomy {

    private final CoffersPlugin plugin;
    private final CoffersEconomy economy;

    CoffersVaultEconomy(final CoffersPlugin plugin, final CoffersEconomy economy) {
        this.plugin = plugin;
        this.economy = economy;
    }

    @Override
    public boolean isEnabled() {
        return this.plugin.isEnabled();
    }

    @Override
    public String getName() {
        return "Coffers";
    }

    @Override
    public boolean hasBankSupport() {
        return false;
    }

    @Override
    public int fractionalDigits() {
        return this.economy.fractionalDigits();
    }

    @Override
    public String format(final double amount) {
        return this.economy.format(BigDecimal.valueOf(amount));
    }

    @Override
    public String currencyNamePlural() {
        return this.economy.currencyNamePlural();
    }

    @Override
    public String currencyNameSingular() {
        return this.economy.currencyNameSingular();
    }

    @Override
    public boolean hasAccount(final String playerName) {
        return hasAccount(Bukkit.getOfflinePlayer(playerName));
    }

    @Override
    public boolean hasAccount(final OfflinePlayer player) {
        return this.economy.hasAccount(player.getUniqueId());
    }

    @Override
    public boolean hasAccount(final String playerName, final String worldName) {
        return hasAccount(playerName);
    }

    @Override
    public boolean hasAccount(final OfflinePlayer player, final String worldName) {
        return hasAccount(player);
    }

    @Override
    public double getBalance(final String playerName) {
        return getBalance(Bukkit.getOfflinePlayer(playerName));
    }

    @Override
    public double getBalance(final OfflinePlayer player) {
        return this.economy.getBalance(player.getUniqueId()).doubleValue();
    }

    @Override
    public double getBalance(final String playerName, final String world) {
        return getBalance(playerName);
    }

    @Override
    public double getBalance(final OfflinePlayer player, final String world) {
        return getBalance(player);
    }

    @Override
    public boolean has(final String playerName, final double amount) {
        return getBalance(playerName) >= amount;
    }

    @Override
    public boolean has(final OfflinePlayer player, final double amount) {
        return getBalance(player) >= amount;
    }

    @Override
    public boolean has(final String playerName, final String worldName, final double amount) {
        return has(playerName, amount);
    }

    @Override
    public boolean has(final OfflinePlayer player, final String worldName, final double amount) {
        return has(player, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(final String playerName, final double amount) {
        return withdrawPlayer(Bukkit.getOfflinePlayer(playerName), amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(final OfflinePlayer player, final double amount) {
        return response(this.economy.withdraw(
                player.getUniqueId(),
                this.economy.defaultCurrencyId(),
                BigDecimal.valueOf(amount),
                TransactionActor.vault("vault-withdraw"),
                "Vault withdraw"
        ));
    }

    @Override
    public EconomyResponse withdrawPlayer(final String playerName, final String worldName, final double amount) {
        return withdrawPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(final OfflinePlayer player, final String worldName, final double amount) {
        return withdrawPlayer(player, amount);
    }

    @Override
    public EconomyResponse depositPlayer(final String playerName, final double amount) {
        return depositPlayer(Bukkit.getOfflinePlayer(playerName), amount);
    }

    @Override
    public EconomyResponse depositPlayer(final OfflinePlayer player, final double amount) {
        return response(this.economy.deposit(
                player.getUniqueId(),
                this.economy.defaultCurrencyId(),
                BigDecimal.valueOf(amount),
                TransactionActor.vault("vault-deposit"),
                "Vault deposit"
        ));
    }

    @Override
    public EconomyResponse depositPlayer(final String playerName, final String worldName, final double amount) {
        return depositPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse depositPlayer(final OfflinePlayer player, final String worldName, final double amount) {
        return depositPlayer(player, amount);
    }

    @Override
    public EconomyResponse createBank(final String name, final String player) {
        return unsupportedBanks();
    }

    @Override
    public EconomyResponse createBank(final String name, final OfflinePlayer player) {
        return unsupportedBanks();
    }

    @Override
    public EconomyResponse deleteBank(final String name) {
        return unsupportedBanks();
    }

    @Override
    public EconomyResponse bankBalance(final String name) {
        return unsupportedBanks();
    }

    @Override
    public EconomyResponse bankHas(final String name, final double amount) {
        return unsupportedBanks();
    }

    @Override
    public EconomyResponse bankWithdraw(final String name, final double amount) {
        return unsupportedBanks();
    }

    @Override
    public EconomyResponse bankDeposit(final String name, final double amount) {
        return unsupportedBanks();
    }

    @Override
    public EconomyResponse isBankOwner(final String name, final String playerName) {
        return unsupportedBanks();
    }

    @Override
    public EconomyResponse isBankOwner(final String name, final OfflinePlayer player) {
        return unsupportedBanks();
    }

    @Override
    public EconomyResponse isBankMember(final String name, final String playerName) {
        return unsupportedBanks();
    }

    @Override
    public EconomyResponse isBankMember(final String name, final OfflinePlayer player) {
        return unsupportedBanks();
    }

    @Override
    public List<String> getBanks() {
        return List.of();
    }

    @Override
    public boolean createPlayerAccount(final String playerName) {
        return createPlayerAccount(Bukkit.getOfflinePlayer(playerName));
    }

    @Override
    public boolean createPlayerAccount(final OfflinePlayer player) {
        this.economy.createAccount(player.getUniqueId());
        return true;
    }

    @Override
    public boolean createPlayerAccount(final String playerName, final String worldName) {
        return createPlayerAccount(playerName);
    }

    @Override
    public boolean createPlayerAccount(final OfflinePlayer player, final String worldName) {
        return createPlayerAccount(player);
    }

    private EconomyResponse response(final TransactionResult result) {
        final EconomyResponse.ResponseType type = result.successful()
                ? EconomyResponse.ResponseType.SUCCESS
                : EconomyResponse.ResponseType.FAILURE;
        return new EconomyResponse(
                result.amount().doubleValue(),
                result.balance().doubleValue(),
                type,
                result.message()
        );
    }

    private EconomyResponse unsupportedBanks() {
        return new EconomyResponse(
                0.0D,
                0.0D,
                EconomyResponse.ResponseType.NOT_IMPLEMENTED,
                "Coffers bank accounts are not implemented yet."
        );
    }
}
