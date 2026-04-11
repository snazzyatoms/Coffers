package com.aegisguard.coffers.legacy;

import java.util.Collections;
import java.util.List;

final class UnavailableLegacyMigrationGateway implements LegacyMigrationGateway {

    private final String message;

    UnavailableLegacyMigrationGateway(final String message) {
        this.message = message;
    }

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public LegacyMigrationReport migrate(final String requestedProviderName) {
        throw new IllegalStateException(this.message);
    }

    @Override
    public List<String> availableProviders() {
        return Collections.emptyList();
    }
}
