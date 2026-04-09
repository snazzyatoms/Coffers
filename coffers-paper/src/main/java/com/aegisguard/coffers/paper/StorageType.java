package com.aegisguard.coffers.paper;

import java.util.Locale;

enum StorageType {
    YAML,
    SQLITE,
    MYSQL;

    static StorageType fromConfig(final String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return YAML;
        }

        try {
            return valueOf(rawValue.trim().toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException exception) {
            return YAML;
        }
    }
}
