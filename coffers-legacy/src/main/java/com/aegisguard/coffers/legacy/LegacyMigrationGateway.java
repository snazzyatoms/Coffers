package com.aegisguard.coffers.legacy;

import java.util.List;

interface LegacyMigrationGateway {

    boolean available();

    LegacyMigrationReport migrate(String requestedProviderName);

    List<String> availableProviders();
}
