package com.aegisguard.coffers.legacy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

interface LegacyEconomyStorage {

    void initialize() throws Exception;

    LegacyStorageSnapshot load() throws Exception;

    void saveAccount(UUID accountId, Map<String, BigDecimal> balances) throws Exception;

    void saveHistory(UUID accountId, List<LegacyLedgerEntry> entries) throws Exception;

    void close() throws Exception;
}
