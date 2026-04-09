package com.aegisguard.coffers.legacy;

final class LegacyMigrationReport {

    private final String providerName;
    private final int importedAccounts;
    private final int updatedAccounts;
    private final int skippedAccounts;

    LegacyMigrationReport(final String providerName, final int importedAccounts, final int updatedAccounts, final int skippedAccounts) {
        this.providerName = providerName;
        this.importedAccounts = importedAccounts;
        this.updatedAccounts = updatedAccounts;
        this.skippedAccounts = skippedAccounts;
    }

    String getProviderName() {
        return this.providerName;
    }

    int getImportedAccounts() {
        return this.importedAccounts;
    }

    int getUpdatedAccounts() {
        return this.updatedAccounts;
    }

    int getSkippedAccounts() {
        return this.skippedAccounts;
    }
}
