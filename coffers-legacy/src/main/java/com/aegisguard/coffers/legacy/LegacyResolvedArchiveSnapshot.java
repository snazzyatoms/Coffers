package com.aegisguard.coffers.legacy;

import java.io.File;

final class LegacyResolvedArchiveSnapshot {

    private final String sourceType;
    private final String sourceName;
    private final File file;
    private final LegacyArchiveSnapshot snapshot;

    LegacyResolvedArchiveSnapshot(
            final String sourceType,
            final String sourceName,
            final File file,
            final LegacyArchiveSnapshot snapshot
    ) {
        this.sourceType = sourceType;
        this.sourceName = sourceName;
        this.file = file;
        this.snapshot = snapshot;
    }

    String sourceType() {
        return this.sourceType;
    }

    String sourceName() {
        return this.sourceName;
    }

    File file() {
        return this.file;
    }

    LegacyArchiveSnapshot snapshot() {
        return this.snapshot;
    }
}
