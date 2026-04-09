package com.aegisguard.coffers.legacy;

import java.math.BigDecimal;

final class LegacyCurrencyDefinition {

    private final String id;
    private final String singularName;
    private final String pluralName;
    private final String symbol;
    private final int fractionalDigits;
    private final BigDecimal startingBalance;
    private final LegacyCurrencyFormat format;

    LegacyCurrencyDefinition(
            final String id,
            final String singularName,
            final String pluralName,
            final String symbol,
            final int fractionalDigits,
            final BigDecimal startingBalance,
            final LegacyCurrencyFormat format
    ) {
        this.id = id;
        this.singularName = singularName;
        this.pluralName = pluralName;
        this.symbol = symbol;
        this.fractionalDigits = fractionalDigits;
        this.startingBalance = startingBalance;
        this.format = format;
    }

    String getId() {
        return this.id;
    }

    String getSingularName() {
        return this.singularName;
    }

    String getPluralName() {
        return this.pluralName;
    }

    String getSymbol() {
        return this.symbol;
    }

    int getFractionalDigits() {
        return this.fractionalDigits;
    }

    BigDecimal getStartingBalance() {
        return this.startingBalance;
    }

    LegacyCurrencyFormat getFormat() {
        return this.format;
    }
}
