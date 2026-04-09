package com.aegisguard.coffers.paper;

record MigrationReport(
        String providerName,
        int importedAccounts,
        int updatedAccounts,
        int skippedAccounts
) {
}
