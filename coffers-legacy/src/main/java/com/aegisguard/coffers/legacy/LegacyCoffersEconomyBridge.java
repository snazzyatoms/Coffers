package com.aegisguard.coffers.legacy;

import com.aegisguard.coffers.spi.NativeCoffersEconomy;
import com.aegisguard.coffers.spi.NativeCoffersTransactionResult;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

final class NativeLegacyCoffersEconomyBridge implements NativeCoffersEconomy {

    private final CoffersLegacyEconomyService economy;

    NativeLegacyCoffersEconomyBridge(final CoffersLegacyEconomyService economy) {
        this.economy = economy;
    }

    @Override
    public String getProviderName() {
        return "CoffersLegacy";
    }

    @Override
    public String getDefaultCurrencyId() {
        return this.economy.getDefaultCurrencyId();
    }

    @Override
    public Collection<String> getCurrencyIds() {
        List<String> ids = new ArrayList<String>();
        for (LegacyCurrencyDefinition currency : this.economy.currencies()) {
            ids.add(currency.getId());
        }
        return ids;
    }

    @Override
    public boolean hasCurrency(final String currencyId) {
        return findCurrency(currencyId) != null;
    }

    @Override
    public String getCurrencySingularName(final String currencyId) {
        return currency(currencyId).getSingularName();
    }

    @Override
    public String getCurrencyPluralName(final String currencyId) {
        return currency(currencyId).getPluralName();
    }

    @Override
    public String getCurrencySymbol(final String currencyId) {
        return currency(currencyId).getSymbol();
    }

    @Override
    public int getFractionalDigits(final String currencyId) {
        return currency(currencyId).getFractionalDigits();
    }

    @Override
    public boolean hasAccount(final UUID accountId) {
        return this.economy.hasAccount(accountId);
    }

    @Override
    public BigDecimal getBalance(final UUID accountId) {
        return this.economy.getBalance(accountId, this.economy.getDefaultCurrencyId());
    }

    @Override
    public BigDecimal getBalance(final UUID accountId, final String currencyId) {
        return this.economy.getBalance(accountId, currencyId);
    }

    @Override
    public NativeCoffersTransactionResult deposit(final UUID accountId, final BigDecimal amount, final String reason) {
        return deposit(accountId, this.economy.getDefaultCurrencyId(), amount, reason);
    }

    @Override
    public NativeCoffersTransactionResult deposit(final UUID accountId, final String currencyId, final BigDecimal amount, final String reason) {
        return translate(this.economy.deposit(accountId, currencyId, amount, systemActor(), reason));
    }

    @Override
    public NativeCoffersTransactionResult withdraw(final UUID accountId, final BigDecimal amount, final String reason) {
        return withdraw(accountId, this.economy.getDefaultCurrencyId(), amount, reason);
    }

    @Override
    public NativeCoffersTransactionResult withdraw(final UUID accountId, final String currencyId, final BigDecimal amount, final String reason) {
        return translate(this.economy.withdraw(accountId, currencyId, amount, systemActor(), reason));
    }

    @Override
    public NativeCoffersTransactionResult transfer(final UUID fromAccountId, final UUID toAccountId, final BigDecimal amount, final String reason) {
        return transfer(fromAccountId, toAccountId, this.economy.getDefaultCurrencyId(), amount, reason);
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
        return setBalance(accountId, this.economy.getDefaultCurrencyId(), amount, reason);
    }

    @Override
    public NativeCoffersTransactionResult setBalance(final UUID accountId, final String currencyId, final BigDecimal amount, final String reason) {
        return translate(this.economy.setBalance(accountId, currencyId, amount, systemActor(), reason));
    }

    @Override
    public String format(final BigDecimal amount) {
        return this.economy.format(this.economy.getDefaultCurrencyId(), amount);
    }

    @Override
    public String format(final String currencyId, final BigDecimal amount) {
        return this.economy.format(currencyId, amount);
    }

    private LegacyCurrencyDefinition currency(final String currencyId) {
        final LegacyCurrencyDefinition currency = findCurrency(currencyId);
        if (currency == null) {
            throw new IllegalArgumentException("Unknown Coffers Legacy currency: " + currencyId);
        }
        return currency;
    }

    private LegacyCurrencyDefinition findCurrency(final String currencyId) {
        for (LegacyCurrencyDefinition currency : this.economy.currencies()) {
            if (currency.getId().equalsIgnoreCase(currencyId)) {
                return currency;
            }
        }
        return null;
    }

    private LegacyTransactionActor systemActor() {
        return LegacyTransactionActor.system("native-coffers");
    }

    private NativeCoffersTransactionResult translate(final LegacyTransactionResult result) {
        return new NativeCoffersTransactionResult(
                result.isSuccessful(),
                result.getCurrencyId(),
                result.getAmount(),
                result.getBalance(),
                result.getMessage()
        );
    }
}
