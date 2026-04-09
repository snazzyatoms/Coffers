package com.aegisguard.coffers.api;

import java.math.BigDecimal;
import java.util.UUID;

public interface CoffersEconomy {

    String currencyNameSingular();

    String currencyNamePlural();

    String currencySymbol();

    int fractionalDigits();

    boolean hasAccount(UUID accountId);

    void createAccount(UUID accountId);

    BigDecimal getBalance(UUID accountId);

    TransactionResult deposit(UUID accountId, BigDecimal amount, String reason);

    TransactionResult withdraw(UUID accountId, BigDecimal amount, String reason);

    TransactionResult transfer(UUID fromAccountId, UUID toAccountId, BigDecimal amount, String reason);

    TransactionResult setBalance(UUID accountId, BigDecimal amount, String reason);

    String format(BigDecimal amount);
}
