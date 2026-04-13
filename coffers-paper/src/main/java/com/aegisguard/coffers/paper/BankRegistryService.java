package com.aegisguard.coffers.paper;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

final class BankRegistryService {

    private final File file;
    private final YamlConfiguration configuration;
    private final Map<String, String> namesByKey = new LinkedHashMap<>();

    BankRegistryService(final JavaPlugin plugin, final String fileName) {
        this.file = new File(plugin.getDataFolder(), fileName);
        this.configuration = YamlConfiguration.loadConfiguration(this.file);
        load();
    }

    synchronized boolean createBank(final String rawName) {
        final String normalized = normalize(rawName);
        if (normalized.isEmpty() || this.namesByKey.containsKey(normalized)) {
            return false;
        }
        this.namesByKey.put(normalized, rawName.trim());
        save();
        return true;
    }

    synchronized boolean deleteBank(final String rawName) {
        final String normalized = normalize(rawName);
        if (!this.namesByKey.containsKey(normalized)) {
            return false;
        }
        this.namesByKey.remove(normalized);
        save();
        return true;
    }

    synchronized boolean exists(final String rawName) {
        return this.namesByKey.containsKey(normalize(rawName));
    }

    synchronized UUID bankAccountId(final String rawName) {
        final String normalized = normalize(rawName);
        if (!this.namesByKey.containsKey(normalized)) {
            throw new IllegalArgumentException("Unknown bank: " + rawName);
        }
        return bankUuid(normalized);
    }

    synchronized boolean isBankAccount(final UUID accountId) {
        for (final String key : this.namesByKey.keySet()) {
            if (bankUuid(key).equals(accountId)) {
                return true;
            }
        }
        return false;
    }

    synchronized String displayName(final UUID accountId) {
        for (final Map.Entry<String, String> entry : this.namesByKey.entrySet()) {
            if (bankUuid(entry.getKey()).equals(accountId)) {
                return entry.getValue();
            }
        }
        return null;
    }

    synchronized Set<String> banks() {
        return new LinkedHashSet<>(this.namesByKey.values());
    }

    synchronized void replaceBanks(final Set<String> bankNames) {
        this.namesByKey.clear();
        for (final String bankName : bankNames) {
            final String normalized = normalize(bankName);
            if (!normalized.isEmpty()) {
                this.namesByKey.put(normalized, bankName.trim());
            }
        }
        save();
    }

    synchronized int bankCount() {
        return this.namesByKey.size();
    }

    private void load() {
        final ConfigurationSection section = this.configuration.getConfigurationSection("banks");
        if (section == null) {
            return;
        }
        for (final String key : section.getKeys(false)) {
            final String displayName = section.getString(key + ".name", key);
            this.namesByKey.put(key, displayName);
        }
    }

    private void save() {
        this.configuration.set("banks", null);
        for (final Map.Entry<String, String> entry : this.namesByKey.entrySet()) {
            this.configuration.set("banks." + entry.getKey() + ".name", entry.getValue());
        }
        try {
            this.configuration.save(this.file);
        } catch (final IOException exception) {
            throw new IllegalStateException("Failed to save Coffers bank registry.", exception);
        }
    }

    private String normalize(final String rawName) {
        return rawName == null ? "" : rawName.trim().toLowerCase(Locale.ROOT);
    }

    private UUID bankUuid(final String normalizedName) {
        return UUID.nameUUIDFromBytes(("coffers-bank:" + normalizedName).getBytes(StandardCharsets.UTF_8));
    }
}
