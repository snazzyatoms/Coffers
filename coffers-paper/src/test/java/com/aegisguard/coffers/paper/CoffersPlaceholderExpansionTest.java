package com.aegisguard.coffers.paper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class CoffersPlaceholderExpansionTest {

    @Test
    void topPlaceholderDefaultsToConfiguredCurrencyWhenOnlyRankIsProvided() {
        final CoffersPlaceholderExpansion.TopPlaceholderQuery query =
                CoffersPlaceholderExpansion.parseTopPlaceholderQuery("top_name_1", "top_name_", "coins");

        assertNotNull(query);
        assertEquals("coins", query.currencyId());
        assertEquals("1", query.rankToken());
    }

    @Test
    void topPlaceholderSupportsCurrencyIdsWithUnderscores() {
        final CoffersPlaceholderExpansion.TopPlaceholderQuery query =
                CoffersPlaceholderExpansion.parseTopPlaceholderQuery("top_balance_bank_notes_3", "top_balance_", "coins");

        assertNotNull(query);
        assertEquals("bank_notes", query.currencyId());
        assertEquals("3", query.rankToken());
    }

    @Test
    void topPlaceholderRejectsInvalidRankTokens() {
        assertNull(CoffersPlaceholderExpansion.parseTopPlaceholderQuery("top_name_bank_notes_final", "top_name_", "coins"));
    }
}
