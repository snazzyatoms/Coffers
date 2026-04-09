package com.aegisguard.coffers.api;

import java.math.BigDecimal;

public record TransactionResult(
        boolean successful,
        String currencyId,
        BigDecimal amount,
        BigDecimal balance,
        TransactionFailure failure,
        String message,
        LedgerEntry entry
) {

    public static TransactionResult success(
            final String currencyId,
            final BigDecimal amount,
            final BigDecimal balance,
            final String message,
            final LedgerEntry entry
    ) {
        return new TransactionResult(true, currencyId, amount, balance, TransactionFailure.NONE, message, entry);
    }

    public static TransactionResult failure(
            final String currencyId,
            final BigDecimal amount,
            final BigDecimal balance,
            final TransactionFailure failure,
            final String message
    ) {
        return new TransactionResult(false, currencyId, amount, balance, failure, message, null);
    }
}
