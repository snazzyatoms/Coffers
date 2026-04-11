package com.aegisguard.coffers.spi;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.UUID;

public interface NativeCoffersEconomy {

    String getProviderName();

    String getDefaultCurrencyId();

    Collection<String> getCurrencyIds();

    boolean hasCurrency(String currencyId);

    String getCurrencySingularName(String currencyId);

    String getCurrencyPluralName(String currencyId);

    String getCurrencySymbol(String currencyId);

    int getFractionalDigits(String currencyId);

    boolean hasAccount(UUID accountId);

    BigDecimal getBalance(UUID accountId);

    BigDecimal getBalance(UUID accountId, String currencyId);

    NativeCoffersTransactionResult deposit(UUID accountId, BigDecimal amount, String reason);

    NativeCoffersTransactionResult deposit(UUID accountId, String currencyId, BigDecimal amount, String reason);

    NativeCoffersTransactionResult withdraw(UUID accountId, BigDecimal amount, String reason);

    NativeCoffersTransactionResult withdraw(UUID accountId, String currencyId, BigDecimal amount, String reason);

    NativeCoffersTransactionResult transfer(UUID fromAccountId, UUID toAccountId, BigDecimal amount, String reason);

    NativeCoffersTransactionResult transfer(UUID fromAccountId, UUID toAccountId, String currencyId, BigDecimal amount, String reason);

    NativeCoffersTransactionResult setBalance(UUID accountId, BigDecimal amount, String reason);

    NativeCoffersTransactionResult setBalance(UUID accountId, String currencyId, BigDecimal amount, String reason);

    String format(BigDecimal amount);

    String format(String currencyId, BigDecimal amount);
}
