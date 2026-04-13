package com.aegisguard.coffers.api;

public enum TransactionFailure {
    NONE,
    INVALID_AMOUNT,
    INSUFFICIENT_FUNDS,
    NOT_FOUND,
    ROLLBACK_UNAVAILABLE
}
