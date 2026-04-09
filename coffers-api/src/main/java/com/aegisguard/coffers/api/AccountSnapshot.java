package com.aegisguard.coffers.api;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountSnapshot(
        UUID accountId,
        String currencyId,
        BigDecimal balance
) {
}
