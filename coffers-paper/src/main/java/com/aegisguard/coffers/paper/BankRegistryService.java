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
        this(new File(plugin.getDataFolder(), fileName));
    }

    BankRegistryService(final File file) {
        this.file = file;
        this.configuration = YamlConfiguration.loadConfiguration(this.file);
        load();
    }

    synchronized boolean createBank(final String rawName) {
        final String normalized = normalize(rawName);
        if (normalized.isEmpty() || lookupKey(rawName) != null) {
            return false;
        }
        this.namesByKey.put(normalized, rawName.trim());
        save();
        return true;
    }

    synchronized boolean deleteBank(final String rawName) {
        final String normalized = lookupKey(rawName);
        if (normalized == null) {
            return false;
        }
        this.namesByKey.remove(normalized);
        save();
        return true;
    }

    synchronized boolean exists(final String rawName) {
        return lookupKey(rawName) != null;
    }

    synchronized UUID bankAccountId(final String rawName) {
        final String normalized = lookupKey(rawName);
        if (normalized == null) {
            throw new IllegalArgumentException("Unknown bank: " + rawName);
        }
        return bankUuid(normalized);
    }

    synchronized String bankKey(final String rawName) {
        return lookupKey(rawName);
    }

    synchronized UUID bankAccountIdForKey(final String rawKey) {
        final String normalized = normalize(rawKey);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Bank key must not be empty.");
        }
        return bankUuid(normalized);
    }

    synchronized UUID ensureBank(final String rawKey, final String displayName) {
        final String normalizedKey = normalize(rawKey);
        if (normalizedKey.isEmpty()) {
            throw new IllegalArgumentException("Bank key must not be empty.");
        }

        final String requestedDisplayName = (displayName == null || displayName.isBlank())
                ? normalizedKey
                : displayName.trim();
        final String resolvedDisplayName = uniqueDisplayName(normalizedKey, requestedDisplayName);
        final String existingDisplayName = this.namesByKey.get(normalizedKey);
        if (!resolvedDisplayName.equals(existingDisplayName)) {
            this.namesByKey.put(normalizedKey, resolvedDisplayName);
            save();
        }
        return bankUuid(normalizedKey);
    }

    synchronized UUID ensurePersonalBank(final UUID ownerId, final String playerName, final String suffix) {
        final String key = personalBankKey(ownerId);
        final String displayName = uniqueDisplayName(key, defaultPersonalBankName(playerName, suffix));
        final String existingDisplayName = this.namesByKey.get(key);
        if (!displayName.equals(existingDisplayName)) {
            this.namesByKey.put(key, displayName);
            save();
        }
        return bankUuid(key);
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

    synchronized Map<String, String> snapshotBanks() {
        return new LinkedHashMap<>(this.namesByKey);
    }

    synchronized void replaceBanks(final Map<String, String> bankNamesByKey) {
        this.namesByKey.clear();
        for (final Map.Entry<String, String> entry : bankNamesByKey.entrySet()) {
            final String normalizedKey = normalize(entry.getKey());
            final String displayName = entry.getValue();
            if (!normalizedKey.isEmpty() && displayName != null && !displayName.isBlank()) {
                this.namesByKey.put(normalizedKey, displayName.trim());
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

    private String lookupKey(final String rawName) {
        final String normalized = normalize(rawName);
        if (normalized.isEmpty()) {
            return null;
        }
        if (this.namesByKey.containsKey(normalized)) {
            return normalized;
        }
        for (final Map.Entry<String, String> entry : this.namesByKey.entrySet()) {
            if (normalize(entry.getValue()).equals(normalized)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private String personalBankKey(final UUID ownerId) {
        return "player-" + ownerId.toString().toLowerCase(Locale.ROOT);
    }

    private String defaultPersonalBankName(final String playerName, final String suffix) {
        final String safePlayerName = playerName == null || playerName.isBlank() ? "player" : playerName.trim();
        final String resolvedSuffix = suffix == null || suffix.isBlank() ? "-bank" : suffix.trim();
        return safePlayerName + resolvedSuffix;
    }

    private String uniqueDisplayName(final String key, final String requestedDisplayName) {
        final String trimmed = requestedDisplayName.trim();
        final String normalizedDisplay = normalize(trimmed);
        for (final Map.Entry<String, String> entry : this.namesByKey.entrySet()) {
            if (!entry.getKey().equals(key) && normalize(entry.getValue()).equals(normalizedDisplay)) {
                return trimmed + "-" + key.substring(Math.max(0, key.length() - 6));
            }
        }
        return trimmed;
    }

    private UUID bankUuid(final String normalizedName) {
        return UUID.nameUUIDFromBytes(("coffers-bank:" + normalizedName).getBytes(StandardCharsets.UTF_8));
    }
}
