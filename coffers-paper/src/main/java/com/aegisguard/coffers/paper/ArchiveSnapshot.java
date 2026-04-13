package com.aegisguard.coffers.paper;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

record ArchiveSnapshot(
        StorageSnapshot storageSnapshot,
        Set<UUID> disabledPaymentAccounts,
        Map<String, String> banks
) {
}
