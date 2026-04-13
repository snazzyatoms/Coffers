package com.aegisguard.coffers.api;

import java.math.BigDecimal;
import java.util.UUID;

public record LedgerEntry(
        UUID entryId,
        UUID referenceId,
        UUID accountId,
        UUID counterpartyAccountId,
        String currencyId,
        TransactionKind kind,
        BigDecimal amount,
        BigDecimal previousBalance,
        BigDecimal resultingBalance,
        TransactionActor actor,
        String reason,
        UUID reversalOfReferenceId,
        long createdAtEpochMilli
) {
}
