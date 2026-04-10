package com.aegisguard.coffers.paper;

import java.util.List;

interface MigrationGateway {

    boolean available();

    MigrationReport migrate(String requestedProviderName);

    List<String> availableProviders();
}
