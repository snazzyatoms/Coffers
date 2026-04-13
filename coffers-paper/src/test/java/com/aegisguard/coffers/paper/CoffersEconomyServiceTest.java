package com.aegisguard.coffers.paper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aegisguard.coffers.api.CurrencyDefinition;
import com.aegisguard.coffers.api.LedgerEntry;
import com.aegisguard.coffers.api.CurrencyFormat;
import com.aegisguard.coffers.api.TransactionActor;
import com.aegisguard.coffers.api.TransactionFailure;
import com.aegisguard.coffers.api.TransactionKind;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Properties;
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

    @Test
    void nullActorIsNormalizedForStandaloneApiCallers() {
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
        final var result = service.deposit(accountId, "coins", new BigDecimal("5.00"), null, "Plugin deposit");

        assertTrue(result.successful());
        assertEquals("System", service.recentTransactions(accountId, 10).getFirst().actor().actorName());
    }

<<<<<<< Updated upstream
=======
    @Test
    void replaceSnapshotPurgesAccountsRemovedFromImportedData() {
        final RecordingStorage storage = new RecordingStorage();
        final UUID keptAccount = UUID.randomUUID();
        final UUID removedAccount = UUID.randomUUID();
        final LedgerEntry removedEntry = new LedgerEntry(
                UUID.randomUUID(),
                UUID.randomUUID(),
                removedAccount,
                null,
                "coins",
                TransactionKind.DEPOSIT,
                new BigDecimal("5.00"),
                new BigDecimal("0.00"),
                new BigDecimal("5.00"),
                TransactionActor.system("test"),
                "Seed",
                null,
                1L
        );

        final CoffersEconomyService service = new CoffersEconomyService(
                List.of(currency("coins")),
                "coins",
                storage,
                10,
                new StorageSnapshot(
                        Map.of(
                                keptAccount, Map.of("coins", new BigDecimal("10.00")),
                                removedAccount, Map.of("coins", new BigDecimal("5.00"))
                        ),
                        Map.of(removedAccount, List.of(removedEntry))
                ),
                Logger.getAnonymousLogger(),
                null
        );

        service.replaceSnapshot(new StorageSnapshot(
                Map.of(keptAccount, Map.of("coins", new BigDecimal("3.00"))),
                Map.of()
        ));

        assertEquals(new BigDecimal("3.00"), service.getBalance(keptAccount, "coins"));
        assertEquals(Map.of(), storage.savedBalances.get(removedAccount));
        assertEquals(List.of(), storage.savedHistory.get(removedAccount));
    }

    @Test
    void sqlStoragePersistsHistoryEntries() throws Exception {
        final Path databaseFile = Files.createTempFile("coffers-sql-storage", ".db");
        final SqlEconomyStorage storage = new SqlEconomyStorage("jdbc:sqlite:" + databaseFile.toAbsolutePath(), new Properties());
        final UUID accountId = UUID.randomUUID();
        final LedgerEntry entry = new LedgerEntry(
                UUID.randomUUID(),
                UUID.randomUUID(),
                accountId,
                null,
                "coins",
                TransactionKind.DEPOSIT,
                new BigDecimal("4.50"),
                new BigDecimal("0.00"),
                new BigDecimal("4.50"),
                TransactionActor.system("test"),
                "Seed",
                null,
                5L
        );

        try {
            storage.initialize();
            storage.saveAccount(accountId, Map.of("coins", new BigDecimal("4.50")));
            storage.saveHistory(accountId, List.of(entry));

            final StorageSnapshot snapshot = storage.load();
            assertEquals(new BigDecimal("4.50"), snapshot.balances().get(accountId).get("coins"));
            assertEquals(1, snapshot.history().get(accountId).size());
            assertEquals(entry.referenceId(), snapshot.history().get(accountId).getFirst().referenceId());
        } finally {
            storage.close();
            Files.deleteIfExists(databaseFile);
        }
    }

>>>>>>> Stashed changes
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

    private static final class RecordingStorage implements EconomyStorage {

        private final Map<UUID, Map<String, BigDecimal>> savedBalances = new HashMap<>();
        private final Map<UUID, List<LedgerEntry>> savedHistory = new HashMap<>();

        @Override
        public void initialize() {
        }

        @Override
        public StorageSnapshot load() {
            return new StorageSnapshot(Map.of(), Map.of());
        }

        @Override
        public void saveAccount(final UUID accountId, final Map<String, BigDecimal> balances) {
            this.savedBalances.put(accountId, new HashMap<>(balances));
        }

        @Override
        public void saveHistory(final UUID accountId, final List<LedgerEntry> entries) {
            this.savedHistory.put(accountId, new ArrayList<>(entries));
        }

        @Override
        public void close() {
        }
    }
}
