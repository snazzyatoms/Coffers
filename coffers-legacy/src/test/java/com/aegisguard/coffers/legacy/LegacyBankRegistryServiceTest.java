package com.aegisguard.coffers.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LegacyBankRegistryServiceTest {

    @Test
    void personalBanksPersistAndResolveByDisplayName() throws Exception {
        final Path directory = Files.createTempDirectory("coffers-legacy-bank-registry");
        final Path file = directory.resolve("legacy-banks.yml");
        final UUID ownerId = UUID.randomUUID();

        try {
            final LegacyBankRegistryService registry = new LegacyBankRegistryService(file.toFile());
            final UUID bankAccountId = registry.ensurePersonalBank(ownerId, "Kai", "-bank");
            assertTrue(registry.exists("Kai-bank"));

            final LegacyBankRegistryService reloaded = new LegacyBankRegistryService(file.toFile());
            assertTrue(reloaded.exists("Kai-bank"));
            assertEquals(bankAccountId, reloaded.bankAccountId("Kai-bank"));
            assertEquals("Kai-bank", reloaded.displayName(bankAccountId));
        } finally {
            deleteRecursively(directory);
        }
    }

    private static void deleteRecursively(final Path root) throws Exception {
        Files.walk(root)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> path.toFile().delete());
    }
}
