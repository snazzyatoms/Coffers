package com.aegisguard.coffers.spi;

import java.math.BigDecimal;

public final class NativeCoffersTransactionResult {

    private final boolean successful;
    private final String currencyId;
    private final BigDecimal amount;
    private final BigDecimal balance;
    private final String message;

    public NativeCoffersTransactionResult(
            final boolean successful,
            final String currencyId,
            final BigDecimal amount,
            final BigDecimal balance,
            final String message
    ) {
        this.successful = successful;
        this.currencyId = currencyId;
        this.amount = amount;
        this.balance = balance;
        this.message = message;
    }

    public boolean isSuccessful() {
        return this.successful;
    }

    public String getCurrencyId() {
        return this.currencyId;
    }

    public BigDecimal getAmount() {
        return this.amount;
    }

    public BigDecimal getBalance() {
        return this.balance;
    }

    public String getMessage() {
        return this.message;
    }
}
