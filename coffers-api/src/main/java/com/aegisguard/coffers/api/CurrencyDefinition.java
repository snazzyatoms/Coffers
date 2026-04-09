package com.aegisguard.coffers.api;

import java.math.BigDecimal;

public record CurrencyDefinition(
        String id,
        String singularName,
        String pluralName,
        String symbol,
        int fractionalDigits,
        BigDecimal startingBalance,
        CurrencyFormat format
) {
}
