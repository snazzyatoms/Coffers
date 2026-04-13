package com.aegisguard.coffers.legacy;

import java.math.BigDecimal;
import java.util.UUID;

final class LegacyLedgerEntry {

    private final UUID entryId;
    private final UUID referenceId;
    private final UUID accountId;
    private final UUID counterpartyAccountId;
    private final String currencyId;
    private final LegacyTransactionKind kind;
    private final BigDecimal amount;
    private final BigDecimal previousBalance;
    private final BigDecimal resultingBalance;
    private final LegacyTransactionActor actor;
    private final String reason;
    private final UUID reversalOfReferenceId;
    private final long createdAtEpochMilli;

    LegacyLedgerEntry(
            final UUID entryId,
            final UUID referenceId,
            final UUID accountId,
            final UUID counterpartyAccountId,
            final String currencyId,
            final LegacyTransactionKind kind,
            final BigDecimal amount,
            final BigDecimal previousBalance,
            final BigDecimal resultingBalance,
            final LegacyTransactionActor actor,
            final String reason,
            final UUID reversalOfReferenceId,
            final long createdAtEpochMilli
    ) {
        this.entryId = entryId;
        this.referenceId = referenceId;
        this.accountId = accountId;
        this.counterpartyAccountId = counterpartyAccountId;
        this.currencyId = currencyId;
        this.kind = kind;
        this.amount = amount;
        this.previousBalance = previousBalance;
        this.resultingBalance = resultingBalance;
        this.actor = actor;
        this.reason = reason;
        this.reversalOfReferenceId = reversalOfReferenceId;
        this.createdAtEpochMilli = createdAtEpochMilli;
    }

    UUID getEntryId() {
        return this.entryId;
    }

    UUID getReferenceId() {
        return this.referenceId;
    }

    UUID getAccountId() {
        return this.accountId;
    }

    UUID getCounterpartyAccountId() {
        return this.counterpartyAccountId;
    }

    String getCurrencyId() {
        return this.currencyId;
    }

    LegacyTransactionKind getKind() {
        return this.kind;
    }

    BigDecimal getAmount() {
        return this.amount;
    }

    BigDecimal getPreviousBalance() {
        return this.previousBalance;
    }

    BigDecimal getResultingBalance() {
        return this.resultingBalance;
    }

    LegacyTransactionActor getActor() {
        return this.actor;
    }

    String getReason() {
        return this.reason;
    }

    UUID getReversalOfReferenceId() {
        return this.reversalOfReferenceId;
    }

    long getCreatedAtEpochMilli() {
        return this.createdAtEpochMilli;
    }
}
