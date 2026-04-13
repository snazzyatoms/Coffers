package com.aegisguard.coffers.legacy;

import java.util.Set;
import java.util.UUID;

final class LegacyArchiveSnapshot {

    private final LegacyStorageSnapshot storageSnapshot;
    private final Set<UUID> disabledPaymentAccounts;
    private final Set<String> banks;

    LegacyArchiveSnapshot(
            final LegacyStorageSnapshot storageSnapshot,
            final Set<UUID> disabledPaymentAccounts,
            final Set<String> banks
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

    Set<String> getBanks() {
        return this.banks;
    }
}
