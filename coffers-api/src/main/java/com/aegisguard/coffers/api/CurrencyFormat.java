package com.aegisguard.coffers.api;

public record CurrencyFormat(
        boolean symbolFirst,
        boolean spaceBetweenSymbolAndAmount,
        boolean spaceBetweenAmountAndName,
        boolean useGrouping,
        boolean showTrailingZeros
) {
}
