package com.aegisguard.coffers.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CoffersEconomyDefaultsTest {

    private static final CurrencyDefinition DEFAULT_CURRENCY = new CurrencyDefinition(
            "coins",
            "coin",
            "coins",
            "$",
            2,
            new BigDecimal("10.00"),
            new CurrencyFormat(true, false, true, true, true)
    );

    @Test
    void defaultHelpersUseTheRegisteredDefaultCurrency() {
        final StubEconomy economy = new StubEconomy();

        assertEquals("coins", economy.defaultCurrencyId());
        assertEquals("coin", economy.currencyNameSingular());
        assertEquals("coins", economy.currencyNamePlural());
        assertEquals("$", economy.currencySymbol());
        assertEquals(2, economy.fractionalDigits());
        assertEquals(new BigDecimal("10.00"), economy.defaultCurrency().startingBalance());
        assertEquals(new BigDecimal("12.34"), economy.getBalance(UUID.randomUUID()));
        assertEquals("coins:4.50", economy.format(new BigDecimal("4.50")));
    }

    private static final class StubEconomy implements CoffersEconomy {

        @Override
        public String defaultCurrencyId() {
            return DEFAULT_CURRENCY.id();
        }

        @Override
        public Collection<CurrencyDefinition> currencies() {
            return List.of(DEFAULT_CURRENCY);
        }

        @Override
        public Optional<CurrencyDefinition> currency(final String currencyId) {
            return DEFAULT_CURRENCY.id().equalsIgnoreCase(currencyId) ? Optional.of(DEFAULT_CURRENCY) : Optional.empty();
        }

        @Override
        public boolean hasAccount(final UUID accountId) {
            return true;
        }

        @Override
        public void createAccount(final UUID accountId) {
        }

        @Override
        public AccountSnapshot account(final UUID accountId, final String currencyId) {
            return new AccountSnapshot(accountId, currencyId, new BigDecimal("12.34"));
        }

        @Override
        public BigDecimal getBalance(final UUID accountId, final String currencyId) {
            return new BigDecimal("12.34");
        }

        @Override
        public TransactionResult deposit(
                final UUID accountId,
                final String currencyId,
                final BigDecimal amount,
                final TransactionActor actor,
                final String reason
        ) {
            return TransactionResult.success(currencyId, amount, amount, reason, null);
        }

        @Override
        public TransactionResult withdraw(
                final UUID accountId,
                final String currencyId,
                final BigDecimal amount,
                final TransactionActor actor,
                final String reason
        ) {
            return TransactionResult.success(currencyId, amount, amount, reason, null);
        }

        @Override
        public TransactionResult transfer(
                final UUID fromAccountId,
                final UUID toAccountId,
                final String currencyId,
                final BigDecimal amount,
                final TransactionActor actor,
                final String reason
        ) {
            return TransactionResult.success(currencyId, amount, amount, reason, null);
        }

        @Override
        public TransactionResult setBalance(
                final UUID accountId,
                final String currencyId,
                final BigDecimal amount,
                final TransactionActor actor,
                final String reason
        ) {
            return TransactionResult.success(currencyId, amount, amount, reason, null);
        }

        @Override
        public List<LedgerEntry> recentTransactions(final UUID accountId, final int limit) {
            return List.of();
        }

        @Override
        public List<LedgerEntry> transactionHistory(final UUID accountId, final int offset, final int limit) {
            return List.of();
        }

        @Override
        public List<AccountSnapshot> topAccounts(final String currencyId, final int limit) {
            return List.of(new AccountSnapshot(UUID.randomUUID(), currencyId, new BigDecimal("99.99")));
        }

        @Override
        public String format(final String currencyId, final BigDecimal amount) {
            return currencyId + ":" + amount.toPlainString();
        }
    }
}
