package com.aegisguard.coffers.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

class CoffersLegacyEconomyServiceTest {

    @Test
    void constructorRejectsUnknownDefaultCurrency() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new CoffersLegacyEconomyService(
                        Arrays.asList(currency("coins")),
                        "gems",
                        new NoOpLegacyStorage(),
                        10,
                        new LegacyStorageSnapshot(Collections.<UUID, Map<String, BigDecimal>>emptyMap(), Collections.<UUID, List<LegacyLedgerEntry>>emptyMap()),
                        Logger.getAnonymousLogger()
                )
        );

        assertTrue(exception.getMessage().contains("Default currency"));
    }

    @Test
    void transferCreatesMatchingLedgerEntriesAndBalances() {
        CoffersLegacyEconomyService service = new CoffersLegacyEconomyService(
                Arrays.asList(currency("coins")),
                "coins",
                new NoOpLegacyStorage(),
                10,
                new LegacyStorageSnapshot(Collections.<UUID, Map<String, BigDecimal>>emptyMap(), Collections.<UUID, List<LegacyLedgerEntry>>emptyMap()),
                Logger.getAnonymousLogger()
        );

        UUID sender = UUID.randomUUID();
        UUID receiver = UUID.randomUUID();

        service.deposit(sender, "coins", new BigDecimal("12.50"), LegacyTransactionActor.system("test"), "Seed");
        LegacyTransactionResult transfer = service.transfer(sender, receiver, "coins", new BigDecimal("2.25"), LegacyTransactionActor.system("test"), "Gift");

        assertTrue(transfer.isSuccessful());
        assertEquals(new BigDecimal("10.25"), service.getBalance(sender, "coins"));
        assertEquals(new BigDecimal("2.25"), service.getBalance(receiver, "coins"));
        assertEquals(1, service.recentTransactions(receiver, 10).size());
        LegacyLedgerEntry senderTransfer = null;
        for (LegacyLedgerEntry entry : service.recentTransactions(sender, 10)) {
            if (entry.getKind() == LegacyTransactionKind.TRANSFER_OUT) {
                senderTransfer = entry;
                break;
            }
        }
        assertTrue(senderTransfer != null);
        assertEquals(
                senderTransfer.getReferenceId(),
                service.recentTransactions(receiver, 10).get(0).getReferenceId()
        );
    }

    @Test
    void invalidAmountsAreRejectedWithoutChangingBalance() {
        CoffersLegacyEconomyService service = new CoffersLegacyEconomyService(
                Arrays.asList(currency("coins")),
                "coins",
                new NoOpLegacyStorage(),
                10,
                new LegacyStorageSnapshot(Collections.<UUID, Map<String, BigDecimal>>emptyMap(), Collections.<UUID, List<LegacyLedgerEntry>>emptyMap()),
                Logger.getAnonymousLogger()
        );

        UUID accountId = UUID.randomUUID();
        LegacyTransactionResult result = service.withdraw(accountId, "coins", new BigDecimal("-1.00"), LegacyTransactionActor.system("test"), "Nope");

        assertEquals(LegacyTransactionFailure.INVALID_AMOUNT, result.getFailure());
        assertEquals(new BigDecimal("0.00"), service.getBalance(accountId, "coins"));
    }

    private static LegacyCurrencyDefinition currency(final String id) {
        return new LegacyCurrencyDefinition(
                id,
                "coin",
                "coins",
                "$",
                2,
                new BigDecimal("0.00"),
                new LegacyCurrencyFormat(true, false, true, true, true)
        );
    }

    private static final class NoOpLegacyStorage implements LegacyEconomyStorage {

        public void initialize() {
        }

        public LegacyStorageSnapshot load() {
            return new LegacyStorageSnapshot(Collections.<UUID, Map<String, BigDecimal>>emptyMap(), Collections.<UUID, List<LegacyLedgerEntry>>emptyMap());
        }

        public void saveAccount(final UUID accountId, final Map<String, BigDecimal> balances) {
        }

        public void saveHistory(final UUID accountId, final List<LegacyLedgerEntry> entries) {
        }

        public void close() {
        }
    }
}
