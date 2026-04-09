package com.aegisguard.coffers.paper;

import com.aegisguard.coffers.api.LedgerEntry;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

record StorageSnapshot(
        Map<UUID, Map<String, BigDecimal>> balances,
        Map<UUID, List<LedgerEntry>> history
) {
}
