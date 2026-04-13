package com.aegisguard.coffers.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LegacySnapshotArchiveServiceTest {

    @Test
    void namedBackupCanBeRestoredDirectly() throws Exception {
        Path dataFolder = Files.createTempDirectory("coffers-legacy-backup-test");
        LegacySnapshotArchiveService service = new LegacySnapshotArchiveService(dataFolder.toFile());
        UUID accountId = UUID.randomUUID();
        UUID disabledPaymentAccount = UUID.randomUUID();
        LegacyStorageSnapshot snapshot = new LegacyStorageSnapshot(
                Collections.<UUID, Map<String, BigDecimal>>singletonMap(accountId, Collections.<String, BigDecimal>singletonMap("coins", new BigDecimal("12.50"))),
                Collections.<UUID, List<LegacyLedgerEntry>>singletonMap(accountId, Collections.singletonList(entry(accountId, "coins", new BigDecimal("12.50"))))
        );

        try {
            java.io.File backupFile = service.backup("spring-restore-point", snapshot, Collections.singleton(disabledPaymentAccount), Collections.singleton("TownBank"));
            assertEquals("spring-restore-point.yml", backupFile.getName());

            LegacyArchiveSnapshot restored = service.restoreBackup("spring-restore-point");
            assertEquals(new BigDecimal("12.50"), restored.getStorageSnapshot().getBalances().get(accountId).get("coins"));
            assertEquals(1, restored.getStorageSnapshot().getHistory().get(accountId).size());
            assertTrue(restored.getDisabledPaymentAccounts().contains(disabledPaymentAccount));
            assertTrue(restored.getBanks().contains("TownBank"));
        } finally {
            deleteRecursively(dataFolder);
        }
    }

    @Test
    void exportAndImportRoundTripPreservesBanksAndPaymentPreferences() throws Exception {
        Path dataFolder = Files.createTempDirectory("coffers-legacy-export-test");
        LegacySnapshotArchiveService service = new LegacySnapshotArchiveService(dataFolder.toFile());
        UUID accountId = UUID.randomUUID();
        UUID disabledPaymentAccount = UUID.randomUUID();
        LegacyStorageSnapshot snapshot = new LegacyStorageSnapshot(
                Collections.<UUID, Map<String, BigDecimal>>singletonMap(accountId, Collections.<String, BigDecimal>singletonMap("coins", new BigDecimal("5.00"))),
                Collections.<UUID, List<LegacyLedgerEntry>>singletonMap(accountId, Collections.singletonList(entry(accountId, "coins", new BigDecimal("5.00"))))
        );

        try {
            service.export("manual-export", snapshot, Collections.singleton(disabledPaymentAccount), Collections.singleton("StaffVault"));

            LegacyArchiveSnapshot imported = service.importSnapshot("manual-export");
            assertNotNull(imported);
            assertEquals(new BigDecimal("5.00"), imported.getStorageSnapshot().getBalances().get(accountId).get("coins"));
            assertEquals(1, imported.getStorageSnapshot().getHistory().get(accountId).size());
            assertTrue(imported.getDisabledPaymentAccounts().contains(disabledPaymentAccount));
            assertTrue(imported.getBanks().contains("StaffVault"));
        } finally {
            deleteRecursively(dataFolder);
        }
    }

    @Test
    void restoreLatestUsesNewestBackupSnapshot() throws Exception {
        Path dataFolder = Files.createTempDirectory("coffers-legacy-latest-backup-test");
        LegacySnapshotArchiveService service = new LegacySnapshotArchiveService(dataFolder.toFile());
        UUID accountId = UUID.randomUUID();

        LegacyStorageSnapshot firstSnapshot = new LegacyStorageSnapshot(
                Collections.<UUID, Map<String, BigDecimal>>singletonMap(accountId, Collections.<String, BigDecimal>singletonMap("coins", new BigDecimal("4.00"))),
                Collections.<UUID, List<LegacyLedgerEntry>>singletonMap(accountId, Collections.singletonList(entry(accountId, "coins", new BigDecimal("4.00"))))
        );
        LegacyStorageSnapshot secondSnapshot = new LegacyStorageSnapshot(
                Collections.<UUID, Map<String, BigDecimal>>singletonMap(accountId, Collections.<String, BigDecimal>singletonMap("coins", new BigDecimal("9.00"))),
                Collections.<UUID, List<LegacyLedgerEntry>>singletonMap(accountId, Collections.singletonList(entry(accountId, "coins", new BigDecimal("9.00"))))
        );

        try {
            java.io.File firstBackup = service.backup("first-pass", firstSnapshot, Collections.<UUID>emptySet(), Collections.singleton("TownBank"));
            java.io.File secondBackup = service.backup("second-pass", secondSnapshot, Collections.<UUID>emptySet(), Collections.unmodifiableSet(new java.util.LinkedHashSet<String>(java.util.Arrays.asList("TownBank", "VaultBank"))));
            assertTrue(secondBackup.setLastModified(firstBackup.lastModified() + 10_000L));

            LegacyArchiveSnapshot restored = service.restoreBackup("latest");
            assertEquals(new BigDecimal("9.00"), restored.getStorageSnapshot().getBalances().get(accountId).get("coins"));
            assertTrue(restored.getBanks().contains("VaultBank"));
        } finally {
            deleteRecursively(dataFolder);
        }
    }

    private static LegacyLedgerEntry entry(final UUID accountId, final String currencyId, final BigDecimal resultingBalance) {
        return new LegacyLedgerEntry(
                UUID.randomUUID(),
                UUID.randomUUID(),
                accountId,
                null,
                currencyId,
                LegacyTransactionKind.DEPOSIT,
                resultingBalance,
                BigDecimal.ZERO.setScale(2),
                resultingBalance,
                LegacyTransactionActor.system("test"),
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
