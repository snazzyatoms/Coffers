package com.aegisguard.coffers.api;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CoffersEconomy {

    String defaultCurrencyId();

    Collection<CurrencyDefinition> currencies();

    Optional<CurrencyDefinition> currency(String currencyId);

    default CurrencyDefinition defaultCurrency() {
        return currency(defaultCurrencyId())
                .orElseThrow(() -> new IllegalStateException("Default currency is not registered: " + defaultCurrencyId()));
    }

    default String currencyNameSingular() {
        return defaultCurrency().singularName();
    }

    default String currencyNamePlural() {
        return defaultCurrency().pluralName();
    }

    default String currencySymbol() {
        return defaultCurrency().symbol();
    }

    default int fractionalDigits() {
        return defaultCurrency().fractionalDigits();
    }

    boolean hasAccount(UUID accountId);

    void createAccount(UUID accountId);

    AccountSnapshot account(UUID accountId, String currencyId);

    default BigDecimal getBalance(final UUID accountId) {
        return getBalance(accountId, defaultCurrencyId());
    }

    BigDecimal getBalance(UUID accountId, String currencyId);

    default TransactionResult deposit(final UUID accountId, final BigDecimal amount, final String reason) {
        return deposit(accountId, defaultCurrencyId(), amount, TransactionActor.system("coffers"), reason);
    }

    TransactionResult deposit(UUID accountId, String currencyId, BigDecimal amount, TransactionActor actor, String reason);

    default TransactionResult withdraw(final UUID accountId, final BigDecimal amount, final String reason) {
        return withdraw(accountId, defaultCurrencyId(), amount, TransactionActor.system("coffers"), reason);
    }

    TransactionResult withdraw(UUID accountId, String currencyId, BigDecimal amount, TransactionActor actor, String reason);

    default TransactionResult transfer(
            final UUID fromAccountId,
            final UUID toAccountId,
            final BigDecimal amount,
            final String reason
    ) {
        return transfer(fromAccountId, toAccountId, defaultCurrencyId(), amount, TransactionActor.system("coffers"), reason);
    }

    TransactionResult transfer(
            UUID fromAccountId,
            UUID toAccountId,
            String currencyId,
            BigDecimal amount,
            TransactionActor actor,
            String reason
    );

    default TransactionResult setBalance(final UUID accountId, final BigDecimal amount, final String reason) {
        return setBalance(accountId, defaultCurrencyId(), amount, TransactionActor.system("coffers"), reason);
    }

    TransactionResult setBalance(UUID accountId, String currencyId, BigDecimal amount, TransactionActor actor, String reason);

    List<LedgerEntry> recentTransactions(UUID accountId, int limit);

    List<LedgerEntry> transactionHistory(UUID accountId, int offset, int limit);

    default List<AccountSnapshot> topAccounts(final int limit) {
        return topAccounts(defaultCurrencyId(), limit);
    }

    List<AccountSnapshot> topAccounts(String currencyId, int limit);

    default String format(final BigDecimal amount) {
        return format(defaultCurrencyId(), amount);
    }

    String format(String currencyId, BigDecimal amount);
}
