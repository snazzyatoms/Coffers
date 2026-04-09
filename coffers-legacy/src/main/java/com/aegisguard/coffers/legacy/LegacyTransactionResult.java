package com.aegisguard.coffers.legacy;

import java.math.BigDecimal;

final class LegacyTransactionResult {

    private final boolean successful;
    private final String currencyId;
    private final BigDecimal amount;
    private final BigDecimal balance;
    private final LegacyTransactionFailure failure;
    private final String message;
    private final LegacyLedgerEntry entry;

    LegacyTransactionResult(
            final boolean successful,
            final String currencyId,
            final BigDecimal amount,
            final BigDecimal balance,
            final LegacyTransactionFailure failure,
            final String message,
            final LegacyLedgerEntry entry
    ) {
        this.successful = successful;
        this.currencyId = currencyId;
        this.amount = amount;
        this.balance = balance;
        this.failure = failure;
        this.message = message;
        this.entry = entry;
    }

    static LegacyTransactionResult success(
            final String currencyId,
            final BigDecimal amount,
            final BigDecimal balance,
            final String message,
            final LegacyLedgerEntry entry
    ) {
        return new LegacyTransactionResult(true, currencyId, amount, balance, LegacyTransactionFailure.NONE, message, entry);
    }

    static LegacyTransactionResult failure(
            final String currencyId,
            final BigDecimal amount,
            final BigDecimal balance,
            final LegacyTransactionFailure failure,
            final String message
    ) {
        return new LegacyTransactionResult(false, currencyId, amount, balance, failure, message, null);
    }

    boolean isSuccessful() {
        return this.successful;
    }

    String getCurrencyId() {
        return this.currencyId;
    }

    BigDecimal getAmount() {
        return this.amount;
    }

    BigDecimal getBalance() {
        return this.balance;
    }

    LegacyTransactionFailure getFailure() {
        return this.failure;
    }

    String getMessage() {
        return this.message;
    }

    LegacyLedgerEntry getEntry() {
        return this.entry;
    }
}
