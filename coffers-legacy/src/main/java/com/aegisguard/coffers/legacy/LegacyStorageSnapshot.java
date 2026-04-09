package com.aegisguard.coffers.legacy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class LegacyStorageSnapshot {

    private final Map<UUID, Map<String, BigDecimal>> balances;
    private final Map<UUID, List<LegacyLedgerEntry>> history;

    LegacyStorageSnapshot(final Map<UUID, Map<String, BigDecimal>> balances, final Map<UUID, List<LegacyLedgerEntry>> history) {
        this.balances = balances;
        this.history = history;
    }

    Map<UUID, Map<String, BigDecimal>> getBalances() {
        return this.balances;
    }

    Map<UUID, List<LegacyLedgerEntry>> getHistory() {
        return this.history;
    }
}
