package com.aegisguard.coffers.paper;

import com.aegisguard.coffers.api.LedgerEntry;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

interface EconomyStorage {

    void initialize() throws Exception;

    StorageSnapshot load() throws Exception;

    void saveAccount(UUID accountId, Map<String, BigDecimal> balances) throws Exception;

    void saveHistory(UUID accountId, List<LedgerEntry> entries) throws Exception;

    void close() throws Exception;
}
