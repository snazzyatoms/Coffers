package com.aegisguard.coffers.legacy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

final class CoffersLegacyConfigValidator {

    private static final Pattern CURRENCY_ID_PATTERN = Pattern.compile("[a-z0-9_-]+");

    private CoffersLegacyConfigValidator() {
    }

    static List<String> validate(final FileConfiguration config) {
        List<String> errors = new ArrayList<String>();

        if (config.getInt("history.max-per-account", 50) < 1) {
            errors.add("history.max-per-account must be 1 or greater.");
        }

        String storageType = config.getString("storage.type", "yaml").toLowerCase(Locale.ROOT);
        if (!Arrays.asList("yaml", "sqlite", "mysql").contains(storageType)) {
            errors.add("storage.type must be one of: yaml, sqlite, mysql.");
        }

        String defaultCurrencyId = config.getString("currencies.default");
        ConfigurationSection definitions = config.getConfigurationSection("currencies.definitions");
        if (definitions == null || definitions.getKeys(false).isEmpty()) {
            errors.add("At least one currency definition must exist under currencies.definitions.");
            return errors;
        }

        boolean defaultFound = false;
        int enabledCurrencies = 0;
        for (String rawCurrencyId : definitions.getKeys(false)) {
            ConfigurationSection section = definitions.getConfigurationSection(rawCurrencyId);
            if (section == null) {
                continue;
            }
            boolean enabled = section.getBoolean("enabled", true);
            if (!enabled) {
                continue;
            }
            enabledCurrencies++;

            String currencyId = rawCurrencyId.toLowerCase(Locale.ROOT);
            if (!CURRENCY_ID_PATTERN.matcher(currencyId).matches()) {
                errors.add("Currency ID '" + rawCurrencyId + "' contains unsupported characters.");
            }
            if (section.getInt("fractional-digits", 0) < 0) {
                errors.add("Currency '" + rawCurrencyId + "' has a negative fractional-digits value.");
            }
            if (section.getString("singular", "").trim().isEmpty()) {
                errors.add("Currency '" + rawCurrencyId + "' must define a singular name.");
            }
            if (section.getString("plural", "").trim().isEmpty()) {
                errors.add("Currency '" + rawCurrencyId + "' must define a plural name.");
            }
            if (defaultCurrencyId != null && currencyId.equalsIgnoreCase(defaultCurrencyId)) {
                defaultFound = true;
            }
        }

        if (enabledCurrencies == 0) {
            errors.add("At least one enabled currency definition is required.");
        }
        if (defaultCurrencyId == null || defaultCurrencyId.trim().isEmpty()) {
            errors.add("currencies.default must be configured.");
        } else if (!defaultFound) {
            errors.add("currencies.default must point to an enabled currency definition.");
        }

        return errors;
    }
}
