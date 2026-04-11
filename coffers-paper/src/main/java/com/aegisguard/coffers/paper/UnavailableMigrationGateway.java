package com.aegisguard.coffers.paper;

import java.util.Collections;
import java.util.List;

final class UnavailableMigrationGateway implements MigrationGateway {

    private final String message;

    UnavailableMigrationGateway(final String message) {
        this.message = message;
    }

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public MigrationReport migrate(final String requestedProviderName) {
        throw new IllegalStateException(this.message);
    }

    @Override
    public List<String> availableProviders() {
        return Collections.emptyList();
    }
}
