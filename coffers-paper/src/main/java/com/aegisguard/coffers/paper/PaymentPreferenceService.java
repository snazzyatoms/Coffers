package com.aegisguard.coffers.paper;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

final class PaymentPreferenceService {

    private final File file;
    private final YamlConfiguration configuration;

    PaymentPreferenceService(final JavaPlugin plugin, final String fileName) {
        this.file = new File(plugin.getDataFolder(), fileName);
        this.configuration = YamlConfiguration.loadConfiguration(this.file);
    }

    synchronized boolean allowsPayments(final UUID accountId) {
        return !this.configuration.getBoolean(path(accountId), false);
    }

    synchronized boolean setAllowsPayments(final UUID accountId, final boolean allowsPayments) {
        this.configuration.set(path(accountId), allowsPayments ? null : Boolean.TRUE);
        save();
        return allowsPayments;
    }

    synchronized boolean togglePayments(final UUID accountId) {
        return setAllowsPayments(accountId, !allowsPayments(accountId));
    }

    synchronized int disabledPaymentCount() {
        final ConfigurationSection section = this.configuration.getConfigurationSection("disabled-payments");
        return section == null ? 0 : section.getKeys(false).size();
    }

    synchronized Set<UUID> disabledPaymentAccounts() {
        final Set<UUID> accountIds = new LinkedHashSet<>();
        final ConfigurationSection section = this.configuration.getConfigurationSection("disabled-payments");
        if (section == null) {
            return accountIds;
        }
        for (final String key : section.getKeys(false)) {
            try {
                accountIds.add(UUID.fromString(key));
            } catch (final IllegalArgumentException ignored) {
                // Skip malformed keys rather than failing the whole preferences file.
            }
        }
        return accountIds;
    }

    synchronized void replaceDisabledPaymentAccounts(final Set<UUID> accountIds) {
        this.configuration.set("disabled-payments", null);
        for (final UUID accountId : accountIds) {
            this.configuration.set(path(accountId), Boolean.TRUE);
        }
        save();
    }

    private String path(final UUID accountId) {
        return "disabled-payments." + accountId;
    }

    private void save() {
        try {
            this.configuration.save(this.file);
        } catch (final IOException exception) {
            throw new IllegalStateException("Failed to save Coffers payment preferences.", exception);
        }
    }
}
