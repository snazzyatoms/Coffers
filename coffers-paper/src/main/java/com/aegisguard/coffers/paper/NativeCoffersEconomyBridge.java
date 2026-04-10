package com.aegisguard.coffers.paper;

import com.aegisguard.coffers.api.CoffersEconomy;
import com.aegisguard.coffers.api.CurrencyDefinition;
import com.aegisguard.coffers.api.TransactionActor;
import com.aegisguard.coffers.api.TransactionResult;
import com.aegisguard.coffers.spi.NativeCoffersEconomy;
import com.aegisguard.coffers.spi.NativeCoffersTransactionResult;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.UUID;

final class NativeCoffersEconomyBridge implements NativeCoffersEconomy {

    private final CoffersEconomy economy;

    NativeCoffersEconomyBridge(final CoffersEconomy economy) {
        this.economy = economy;
    }

    @Override
    public String getProviderName() {
        return "Coffers";
    }

    @Override
    public String getDefaultCurrencyId() {
        return this.economy.defaultCurrencyId();
    }

    @Override
    public Collection<String> getCurrencyIds() {
        return this.economy.currencies().stream().map(CurrencyDefinition::id).toList();
    }

    @Override
    public boolean hasCurrency(final String currencyId) {
        return this.economy.currency(currencyId).isPresent();
    }

    @Override
    public String getCurrencySingularName(final String currencyId) {
        return currency(currencyId).singularName();
    }

    @Override
    public String getCurrencyPluralName(final String currencyId) {
        return currency(currencyId).pluralName();
    }

    @Override
    public String getCurrencySymbol(final String currencyId) {
        return currency(currencyId).symbol();
    }

    @Override
    public int getFractionalDigits(final String currencyId) {
        return currency(currencyId).fractionalDigits();
    }

    @Override
    public boolean hasAccount(final UUID accountId) {
        return this.economy.hasAccount(accountId);
    }

    @Override
    public BigDecimal getBalance(final UUID accountId) {
        return this.economy.getBalance(accountId);
    }

    @Override
    public BigDecimal getBalance(final UUID accountId, final String currencyId) {
        return this.economy.getBalance(accountId, currencyId);
    }

    @Override
    public NativeCoffersTransactionResult deposit(final UUID accountId, final BigDecimal amount, final String reason) {
        return deposit(accountId, this.economy.defaultCurrencyId(), amount, reason);
    }

    @Override
    public NativeCoffersTransactionResult deposit(final UUID accountId, final String currencyId, final BigDecimal amount, final String reason) {
        return translate(this.economy.deposit(accountId, currencyId, amount, systemActor(), reason));
    }

    @Override
    public NativeCoffersTransactionResult withdraw(final UUID accountId, final BigDecimal amount, final String reason) {
        return withdraw(accountId, this.economy.defaultCurrencyId(), amount, reason);
    }

    @Override
    public NativeCoffersTransactionResult withdraw(final UUID accountId, final String currencyId, final BigDecimal amount, final String reason) {
        return translate(this.economy.withdraw(accountId, currencyId, amount, systemActor(), reason));
    }

    @Override
    public NativeCoffersTransactionResult transfer(final UUID fromAccountId, final UUID toAccountId, final BigDecimal amount, final String reason) {
        return transfer(fromAccountId, toAccountId, this.economy.defaultCurrencyId(), amount, reason);
    }

    @Override
    public NativeCoffersTransactionResult transfer(
            final UUID fromAccountId,
            final UUID toAccountId,
            final String currencyId,
            final BigDecimal amount,
            final String reason
    ) {
        return translate(this.economy.transfer(fromAccountId, toAccountId, currencyId, amount, systemActor(), reason));
    }

    @Override
    public NativeCoffersTransactionResult setBalance(final UUID accountId, final BigDecimal amount, final String reason) {
        return setBalance(accountId, this.economy.defaultCurrencyId(), amount, reason);
    }

    @Override
    public NativeCoffersTransactionResult setBalance(final UUID accountId, final String currencyId, final BigDecimal amount, final String reason) {
        return translate(this.economy.setBalance(accountId, currencyId, amount, systemActor(), reason));
    }

    @Override
    public String format(final BigDecimal amount) {
        return this.economy.format(amount);
    }

    @Override
    public String format(final String currencyId, final BigDecimal amount) {
        return this.economy.format(currencyId, amount);
    }

    private CurrencyDefinition currency(final String currencyId) {
        return this.economy.currency(currencyId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown Coffers currency: " + currencyId));
    }

    private TransactionActor systemActor() {
        return TransactionActor.system("native-coffers");
    }

    private NativeCoffersTransactionResult translate(final TransactionResult result) {
        return new NativeCoffersTransactionResult(
                result.successful(),
                result.currencyId(),
                result.amount(),
                result.balance(),
                result.message()
        );
    }
}
