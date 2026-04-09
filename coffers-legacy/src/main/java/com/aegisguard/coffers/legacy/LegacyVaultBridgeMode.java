package com.aegisguard.coffers.legacy;

import java.util.Locale;

enum LegacyVaultBridgeMode {
    AUTO,
    ENABLED,
    DISABLED;

    static LegacyVaultBridgeMode fromConfig(final String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return AUTO;
        }

        try {
            return valueOf(rawValue.trim().toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException exception) {
            return AUTO;
        }
    }
}
