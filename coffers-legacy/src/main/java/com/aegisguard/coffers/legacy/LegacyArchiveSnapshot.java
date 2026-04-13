package com.aegisguard.coffers.legacy;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class LegacyArchiveSnapshot {

    private final LegacyStorageSnapshot storageSnapshot;
    private final Set<UUID> disabledPaymentAccounts;
    private final Map<String, String> banks;

    LegacyArchiveSnapshot(
            final LegacyStorageSnapshot storageSnapshot,
            final Set<UUID> disabledPaymentAccounts,
            final Map<String, String> banks
    ) {
        this.storageSnapshot = storageSnapshot;
        this.disabledPaymentAccounts = disabledPaymentAccounts;
        this.banks = banks;
    }

    LegacyStorageSnapshot getStorageSnapshot() {
        return this.storageSnapshot;
    }

    Set<UUID> getDisabledPaymentAccounts() {
        return this.disabledPaymentAccounts;
    }

    Map<String, String> getBanks() {
        return this.banks;
    }
}
