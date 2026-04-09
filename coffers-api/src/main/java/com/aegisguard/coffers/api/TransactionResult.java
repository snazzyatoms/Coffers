package com.aegisguard.coffers.api;

import java.math.BigDecimal;

public record TransactionResult(
        boolean successful,
        BigDecimal amount,
        BigDecimal balance,
        TransactionFailure failure,
        String message
) {

    public static TransactionResult success(final BigDecimal amount, final BigDecimal balance, final String message) {
        return new TransactionResult(true, amount, balance, TransactionFailure.NONE, message);
    }

    public static TransactionResult failure(
            final BigDecimal amount,
            final BigDecimal balance,
            final TransactionFailure failure,
            final String message
    ) {
        return new TransactionResult(false, amount, balance, failure, message);
    }
}
