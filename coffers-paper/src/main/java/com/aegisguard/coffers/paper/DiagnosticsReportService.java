package com.aegisguard.coffers.paper;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

final class DiagnosticsReportService {

    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final JavaPlugin plugin;

    DiagnosticsReportService(final JavaPlugin plugin) {
        this.plugin = plugin;
    }

    void writeStartupReport(
            final String jarLine,
            final String storageType,
            final String defaultCurrency,
            final int currencyCount,
            final String vaultMode,
            final String vaultState,
            final boolean placeholderActive,
            final int disabledPayments,
            final int bankCount
    ) {
        final YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("type", "startup");
        configuration.set("created-at", System.currentTimeMillis());
        configuration.set("jar-line", jarLine);
        configuration.set("storage", storageType);
        configuration.set("default-currency", defaultCurrency);
        configuration.set("currency-count", currencyCount);
        configuration.set("vault.mode", vaultMode);
        configuration.set("vault.state", vaultState);
        configuration.set("placeholderapi", placeholderActive);
        configuration.set("disabled-payments", disabledPayments);
        configuration.set("bank-count", bankCount);
        save(configuration, new File(reportDirectory(), "startup-latest.yml"));
    }

    void writeMigrationReport(
            final String requestedProvider,
            final MigrationReport report,
            final Throwable error,
            final List<String> availableProviders
    ) {
        final YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("type", "migration");
        configuration.set("created-at", System.currentTimeMillis());
        configuration.set("requested-provider", requestedProvider);
        if (report != null) {
            configuration.set("successful", Boolean.TRUE);
            configuration.set("provider", report.providerName());
            configuration.set("imported-accounts", report.importedAccounts());
            configuration.set("updated-accounts", report.updatedAccounts());
            configuration.set("skipped-accounts", report.skippedAccounts());
        } else {
            configuration.set("successful", Boolean.FALSE);
            configuration.set("error", error == null ? "Unknown migration failure." : error.getMessage());
        }
        configuration.set("available-providers", availableProviders);
        save(configuration, new File(reportDirectory(), "migration-" + FILE_STAMP.format(LocalDateTime.now()) + ".yml"));
    }

    private File reportDirectory() {
        final File directory = new File(this.plugin.getDataFolder(), "reports");
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IllegalStateException("Could not create Coffers reports directory.");
        }
        return directory;
    }

    private void save(final YamlConfiguration configuration, final File file) {
        try {
            configuration.save(file);
        } catch (final IOException exception) {
            throw new IllegalStateException("Failed to write Coffers report file.", exception);
        }
    }
}
