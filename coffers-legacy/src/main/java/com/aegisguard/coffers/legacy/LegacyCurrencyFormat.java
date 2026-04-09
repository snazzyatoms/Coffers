package com.aegisguard.coffers.legacy;

final class LegacyCurrencyFormat {

    private final boolean symbolFirst;
    private final boolean spaceBetweenSymbolAndAmount;
    private final boolean spaceBetweenAmountAndName;
    private final boolean useGrouping;
    private final boolean showTrailingZeros;

    LegacyCurrencyFormat(
            final boolean symbolFirst,
            final boolean spaceBetweenSymbolAndAmount,
            final boolean spaceBetweenAmountAndName,
            final boolean useGrouping,
            final boolean showTrailingZeros
    ) {
        this.symbolFirst = symbolFirst;
        this.spaceBetweenSymbolAndAmount = spaceBetweenSymbolAndAmount;
        this.spaceBetweenAmountAndName = spaceBetweenAmountAndName;
        this.useGrouping = useGrouping;
        this.showTrailingZeros = showTrailingZeros;
    }

    boolean isSymbolFirst() {
        return this.symbolFirst;
    }

    boolean isSpaceBetweenSymbolAndAmount() {
        return this.spaceBetweenSymbolAndAmount;
    }

    boolean isSpaceBetweenAmountAndName() {
        return this.spaceBetweenAmountAndName;
    }

    boolean isUseGrouping() {
        return this.useGrouping;
    }

    boolean isShowTrailingZeros() {
        return this.showTrailingZeros;
    }
}
