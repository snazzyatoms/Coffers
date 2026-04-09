package com.aegisguard.coffers.paper;

import java.util.Locale;

enum VaultBridgeMode {
    AUTO,
    ENABLED,
    DISABLED;

    static VaultBridgeMode fromConfig(final String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return AUTO;
        }

        try {
            return valueOf(rawValue.trim().toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException exception) {
            return AUTO;
        }
    }
}
