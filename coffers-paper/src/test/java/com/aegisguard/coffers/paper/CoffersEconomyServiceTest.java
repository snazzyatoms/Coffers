package com.aegisguard.coffers.paper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aegisguard.coffers.api.CurrencyDefinition;
import com.aegisguard.coffers.api.CurrencyFormat;
import com.aegisguard.coffers.api.TransactionActor;
import com.aegisguard.coffers.api.TransactionFailure;
import com.aegisguard.coffers.api.TransactionKind;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

class CoffersEconomyServiceTest {

    @Test
    void constructorRejectsUnknownDefaultCurrency() {
        final IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new CoffersEconomyService(
                        List.of(currency("coins")),
                        "gems",
                        new NoOpStorage(),
                        10,
                        new StorageSnapshot(Map.of(), Map.of()),
                        Logger.getAnonymousLogger(),
                        null
                )
        );

        assertTrue(exception.getMessage().contains("Default currency"));
    }

    @Test
    void transferCreatesMatchingLedgerEntriesAndBalances() {
        final CoffersEconomyService service = new CoffersEconomyService(
                List.of(currency("coins")),
                "coins",
                new NoOpStorage(),
                10,
                new StorageSnapshot(Map.of(), Map.of()),
                Logger.getAnonymousLogger(),
                null
        );

        final UUID sender = UUID.randomUUID();
        final UUID receiver = UUID.randomUUID();

        service.deposit(sender, "coins", new BigDecimal("12.50"), TransactionActor.system("test"), "Seed");
        final var transfer = service.transfer(sender, receiver, "coins", new BigDecimal("2.25"), TransactionActor.system("test"), "Gift");

        assertTrue(transfer.successful());
        assertEquals(new BigDecimal("10.25"), service.getBalance(sender, "coins"));
        assertEquals(new BigDecimal("2.25"), service.getBalance(receiver, "coins"));
        assertEquals(1, service.recentTransactions(receiver, 10).size());
        final var senderTransfer = service.recentTransactions(sender, 10).stream()
                .filter(entry -> entry.kind() == TransactionKind.TRANSFER_OUT)
                .findFirst()
                .orElseThrow();
        assertEquals(
                senderTransfer.referenceId(),
                service.recentTransactions(receiver, 10).getFirst().referenceId()
        );
    }

    @Test
    void invalidAmountsAreRejectedWithoutChangingBalance() {
        final CoffersEconomyService service = new CoffersEconomyService(
                List.of(currency("coins")),
                "coins",
                new NoOpStorage(),
                10,
                new StorageSnapshot(Map.of(), Map.of()),
                Logger.getAnonymousLogger(),
                null
        );

        final UUID accountId = UUID.randomUUID();
        final var result = service.withdraw(accountId, "coins", new BigDecimal("-1.00"), TransactionActor.system("test"), "Nope");

        assertEquals(TransactionFailure.INVALID_AMOUNT, result.failure());
        assertEquals(new BigDecimal("0.00"), service.getBalance(accountId, "coins"));
    }

    private static CurrencyDefinition currency(final String id) {
        return new CurrencyDefinition(
                id,
                "coin",
                "coins",
                "$",
                2,
                new BigDecimal("0.00"),
                new CurrencyFormat(true, false, true, true, true)
        );
    }

    private static final class NoOpStorage implements EconomyStorage {

        @Override
        public void initialize() {
        }

        @Override
        public StorageSnapshot load() {
            return new StorageSnapshot(Map.of(), Map.of());
        }

        @Override
        public void saveAccount(final UUID accountId, final Map<String, BigDecimal> balances) {
        }

        @Override
        public void saveHistory(final UUID accountId, final List<com.aegisguard.coffers.api.LedgerEntry> entries) {
        }

        @Override
        public void close() {
        }
    }
}
