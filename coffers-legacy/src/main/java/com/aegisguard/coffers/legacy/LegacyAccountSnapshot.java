package com.aegisguard.coffers.legacy;

import java.math.BigDecimal;
import java.util.UUID;

final class LegacyAccountSnapshot {

    private final UUID accountId;
    private final String currencyId;
    private final BigDecimal balance;

    LegacyAccountSnapshot(final UUID accountId, final String currencyId, final BigDecimal balance) {
        this.accountId = accountId;
        this.currencyId = currencyId;
        this.balance = balance;
    }

    UUID getAccountId() {
        return this.accountId;
    }

    String getCurrencyId() {
        return this.currencyId;
    }

    BigDecimal getBalance() {
        return this.balance;
    }
}
