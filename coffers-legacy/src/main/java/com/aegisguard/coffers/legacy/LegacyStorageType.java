package com.aegisguard.coffers.legacy;

import java.util.Locale;

enum LegacyStorageType {
    YAML,
    SQLITE,
    MYSQL;

    static LegacyStorageType fromConfig(final String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return YAML;
        }

        try {
            return valueOf(rawValue.trim().toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException exception) {
            return YAML;
        }
    }
}
