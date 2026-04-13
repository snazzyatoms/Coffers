package com.aegisguard.coffers.paper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aegisguard.coffers.api.LedgerEntry;
import com.aegisguard.coffers.api.TransactionActor;
import com.aegisguard.coffers.api.TransactionKind;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SnapshotArchiveServiceTest {

    @Test
    void namedBackupCanBeRestoredDirectly() throws Exception {
        final Path dataFolder = Files.createTempDirectory("coffers-backup-test");
        final SnapshotArchiveService service = new SnapshotArchiveService(dataFolder.toFile());
        final UUID accountId = UUID.randomUUID();
        final UUID disabledPaymentAccount = UUID.randomUUID();
        final StorageSnapshot snapshot = new StorageSnapshot(
                Map.of(accountId, Map.of("coins", new BigDecimal("12.50"))),
                Map.of(accountId, List.of(entry(accountId, "coins", new BigDecimal("12.50"))))
        );

        try {
            final var backupFile = service.backup("spring-restore-point", snapshot, Set.of(disabledPaymentAccount), Set.of("TownBank"));
            assertEquals("spring-restore-point.yml", backupFile.getName());

            final ArchiveSnapshot restored = service.restoreBackup("spring-restore-point");
            assertEquals(new BigDecimal("12.50"), restored.storageSnapshot().balances().get(accountId).get("coins"));
            assertEquals(1, restored.storageSnapshot().history().get(accountId).size());
            assertTrue(restored.disabledPaymentAccounts().contains(disabledPaymentAccount));
            assertTrue(restored.banks().contains("TownBank"));
        } finally {
            deleteRecursively(dataFolder);
        }
    }

    @Test
    void exportAndImportRoundTripPreservesBanksAndPaymentPreferences() throws Exception {
        final Path dataFolder = Files.createTempDirectory("coffers-export-test");
        final SnapshotArchiveService service = new SnapshotArchiveService(dataFolder.toFile());
        final UUID accountId = UUID.randomUUID();
        final UUID disabledPaymentAccount = UUID.randomUUID();
        final StorageSnapshot snapshot = new StorageSnapshot(
                Map.of(accountId, Map.of("coins", new BigDecimal("5.00"))),
                Map.of(accountId, List.of(entry(accountId, "coins", new BigDecimal("5.00"))))
        );

        try {
            service.export("manual-export", snapshot, Set.of(disabledPaymentAccount), Set.of("StaffVault"));

            final ArchiveSnapshot imported = service.importSnapshot("manual-export");
            assertNotNull(imported);
            assertEquals(new BigDecimal("5.00"), imported.storageSnapshot().balances().get(accountId).get("coins"));
            assertEquals(1, imported.storageSnapshot().history().get(accountId).size());
            assertTrue(imported.disabledPaymentAccounts().contains(disabledPaymentAccount));
            assertTrue(imported.banks().contains("StaffVault"));
        } finally {
            deleteRecursively(dataFolder);
        }
    }

    @Test
    void restoreLatestUsesNewestBackupSnapshot() throws Exception {
        final Path dataFolder = Files.createTempDirectory("coffers-latest-backup-test");
        final SnapshotArchiveService service = new SnapshotArchiveService(dataFolder.toFile());
        final UUID accountId = UUID.randomUUID();

        final StorageSnapshot firstSnapshot = new StorageSnapshot(
                Map.of(accountId, Map.of("coins", new BigDecimal("4.00"))),
                Map.of(accountId, List.of(entry(accountId, "coins", new BigDecimal("4.00"))))
        );
        final StorageSnapshot secondSnapshot = new StorageSnapshot(
                Map.of(accountId, Map.of("coins", new BigDecimal("9.00"))),
                Map.of(accountId, List.of(entry(accountId, "coins", new BigDecimal("9.00"))))
        );

        try {
            final var firstBackup = service.backup("first-pass", firstSnapshot, Set.of(), Set.of("TownBank"));
            final var secondBackup = service.backup("second-pass", secondSnapshot, Set.of(), Set.of("TownBank", "VaultBank"));
            assertTrue(secondBackup.setLastModified(firstBackup.lastModified() + 10_000L));

            final ArchiveSnapshot restored = service.restoreBackup("latest");
            assertEquals(new BigDecimal("9.00"), restored.storageSnapshot().balances().get(accountId).get("coins"));
            assertTrue(restored.banks().contains("VaultBank"));
        } finally {
            deleteRecursively(dataFolder);
        }
    }

    private static LedgerEntry entry(final UUID accountId, final String currencyId, final BigDecimal resultingBalance) {
        return new LedgerEntry(
                UUID.randomUUID(),
                UUID.randomUUID(),
                accountId,
                null,
                currencyId,
                TransactionKind.DEPOSIT,
                resultingBalance,
                BigDecimal.ZERO.setScale(2),
                resultingBalance,
                TransactionActor.system("test"),
                "Seed",
                null,
                1L
        );
    }

    private static void deleteRecursively(final Path root) throws Exception {
        Files.walk(root)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> path.toFile().delete());
    }
}
