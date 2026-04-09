package com.aegisguard.coffers.paper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

final class CoffersConfigValidator {

    private static final Pattern CURRENCY_ID_PATTERN = Pattern.compile("[a-z0-9_-]+");

    private CoffersConfigValidator() {
    }

    static List<String> validate(final FileConfiguration config) {
        final List<String> errors = new ArrayList<>();

        if (config.getInt("history.max-per-account", 50) < 1) {
            errors.add("history.max-per-account must be 1 or greater.");
        }

        final String storageType = config.getString("storage.type", "yaml").toLowerCase(Locale.ROOT);
        if (!List.of("yaml", "sqlite", "mysql").contains(storageType)) {
            errors.add("storage.type must be one of: yaml, sqlite, mysql.");
        }

        final String defaultCurrencyId = config.getString("currencies.default");
        final ConfigurationSection definitions = config.getConfigurationSection("currencies.definitions");
        if (definitions == null || definitions.getKeys(false).isEmpty()) {
            errors.add("At least one currency definition must exist under currencies.definitions.");
            return errors;
        }

        boolean defaultFound = false;
        int enabledCurrencies = 0;
        for (final String rawCurrencyId : definitions.getKeys(false)) {
            final ConfigurationSection section = definitions.getConfigurationSection(rawCurrencyId);
            if (section == null) {
                continue;
            }
            final boolean enabled = section.getBoolean("enabled", true);
            if (!enabled) {
                continue;
            }
            enabledCurrencies++;

            final String currencyId = rawCurrencyId.toLowerCase(Locale.ROOT);
            if (!CURRENCY_ID_PATTERN.matcher(currencyId).matches()) {
                errors.add("Currency ID '" + rawCurrencyId + "' contains unsupported characters.");
            }
            if (section.getInt("fractional-digits", 0) < 0) {
                errors.add("Currency '" + rawCurrencyId + "' has a negative fractional-digits value.");
            }
            if (section.getString("singular", "").isBlank()) {
                errors.add("Currency '" + rawCurrencyId + "' must define a singular name.");
            }
            if (section.getString("plural", "").isBlank()) {
                errors.add("Currency '" + rawCurrencyId + "' must define a plural name.");
            }
            if (currencyId.equalsIgnoreCase(defaultCurrencyId)) {
                defaultFound = true;
            }
        }

        if (enabledCurrencies == 0) {
            errors.add("At least one enabled currency definition is required.");
        }
        if (defaultCurrencyId == null || defaultCurrencyId.isBlank()) {
            errors.add("currencies.default must be configured.");
        } else if (!defaultFound) {
            errors.add("currencies.default must point to an enabled currency definition.");
        }

        return errors;
    }
}
