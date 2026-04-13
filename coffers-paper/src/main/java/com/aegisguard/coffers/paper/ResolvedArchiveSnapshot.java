package com.aegisguard.coffers.paper;

import java.io.File;

record ResolvedArchiveSnapshot(
        String sourceType,
        String sourceName,
        File file,
        ArchiveSnapshot snapshot
) {
}
