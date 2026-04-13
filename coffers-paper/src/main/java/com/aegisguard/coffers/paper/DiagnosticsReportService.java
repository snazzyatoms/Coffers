package com.aegisguard.coffers.paper;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
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

    File writeBackupReport(
            final String trigger,
            final File snapshotFile,
            final int prunedAutomatedBackups,
            final int accountCount,
            final int historyAccountCount,
            final int disabledPayments,
            final int bankCount
    ) {
        final YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("type", "backup");
        configuration.set("created-at", System.currentTimeMillis());
        configuration.set("trigger", trigger);
        configuration.set("file.name", snapshotFile.getName());
        configuration.set("file.path", snapshotFile.getAbsolutePath());
        configuration.set("pruned-automated-backups", prunedAutomatedBackups);
        configuration.set("account-count", accountCount);
        configuration.set("history-account-count", historyAccountCount);
        configuration.set("disabled-payments", disabledPayments);
        configuration.set("bank-count", bankCount);
        final File file = new File(reportDirectory(), "backup-" + FILE_STAMP.format(LocalDateTime.now()) + ".yml");
        save(configuration, file);
        return file;
    }

    File writeValidationReport(
            final String scope,
            final int balanceAccountCount,
            final int historyAccountCount,
            final int disabledPayments,
            final int bankCount,
            final List<String> issues
    ) {
        final YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("type", "validation");
        configuration.set("created-at", System.currentTimeMillis());
        configuration.set("scope", scope);
        configuration.set("healthy", issues.isEmpty());
        configuration.set("balance-account-count", balanceAccountCount);
        configuration.set("history-account-count", historyAccountCount);
        configuration.set("disabled-payments", disabledPayments);
        configuration.set("bank-count", bankCount);
        configuration.set("issues", issues);
        final File file = new File(reportDirectory(), "validation-" + FILE_STAMP.format(LocalDateTime.now()) + ".yml");
        save(configuration, file);
        return file;
    }

    File writeRestorePreviewReport(
            final String scope,
            final String sourceType,
            final String sourceName,
            final Map<String, Object> summary,
            final List<String> notes
    ) {
        final YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("type", "restore-preview");
        configuration.set("created-at", System.currentTimeMillis());
        configuration.set("scope", scope);
        configuration.set("source.type", sourceType);
        configuration.set("source.name", sourceName);
        for (final Map.Entry<String, Object> entry : summary.entrySet()) {
            configuration.set("summary." + entry.getKey(), entry.getValue());
        }
        configuration.set("notes", notes);
        final File file = new File(reportDirectory(), "restore-preview-" + FILE_STAMP.format(LocalDateTime.now()) + ".yml");
        save(configuration, file);
        return file;
    }

    File writeAccountRestoreReport(
            final String target,
            final String sourceType,
            final String sourceName,
            final boolean dryRun,
            final Map<String, Object> summary,
            final List<String> notes
    ) {
        final YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("type", "account-restore");
        configuration.set("created-at", System.currentTimeMillis());
        configuration.set("target", target);
        configuration.set("source.type", sourceType);
        configuration.set("source.name", sourceName);
        configuration.set("dry-run", dryRun);
        for (final Map.Entry<String, Object> entry : summary.entrySet()) {
            configuration.set("summary." + entry.getKey(), entry.getValue());
        }
        configuration.set("notes", notes);
        final File file = new File(reportDirectory(), "account-restore-" + FILE_STAMP.format(LocalDateTime.now()) + ".yml");
        save(configuration, file);
        return file;
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
