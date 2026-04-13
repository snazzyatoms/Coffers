package com.aegisguard.coffers.legacy;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import org.bukkit.scheduler.BukkitTask;

final class LegacySafetyBackupService {

    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final List<String> AUTOMATED_PREFIXES = Arrays.asList(
            "coffers-legacy-auto-",
            "coffers-legacy-startup-",
            "coffers-legacy-shutdown-"
    );

    private final CoffersLegacyPlugin plugin;
    private BukkitTask scheduledTask;

    LegacySafetyBackupService(final CoffersLegacyPlugin plugin) {
        this.plugin = plugin;
    }

    void start() {
        stop();
        if (!this.plugin.autoBackupsEnabled()) {
            return;
        }

        final int intervalMinutes = Math.max(1, this.plugin.autoBackupIntervalMinutes());
        final long intervalTicks = intervalMinutes * 60L * 20L;
        this.scheduledTask = this.plugin.getServer().getScheduler().runTaskTimer(
                this.plugin,
                () -> runSafetyBackup("scheduled", "coffers-legacy-auto-"),
                intervalTicks,
                intervalTicks
        );
    }

    void stop() {
        if (this.scheduledTask != null) {
            this.scheduledTask.cancel();
            this.scheduledTask = null;
        }
    }

    void runStartupBackupIfEnabled() {
        if (this.plugin.startupSafetyBackupEnabled()) {
            runSafetyBackup("startup", "coffers-legacy-startup-");
        }
    }

    void runShutdownBackupIfEnabled() {
        if (this.plugin.shutdownSafetyBackupEnabled()) {
            runSafetyBackup("shutdown", "coffers-legacy-shutdown-");
        }
    }

    private void runSafetyBackup(final String trigger, final String filePrefix) {
        if (this.plugin.economy() == null || this.plugin.paymentPreferences() == null || this.plugin.bankRegistry() == null) {
            return;
        }

        try {
            LegacyStorageSnapshot snapshot = this.plugin.economy().snapshot();
            File backupFile = this.plugin.archiveService().backup(
                    filePrefix + FILE_STAMP.format(LocalDateTime.now()),
                    snapshot,
                    this.plugin.paymentPreferences().disabledPaymentAccounts(),
                    this.plugin.bankRegistry().snapshotBanks()
            );
            int pruned = this.plugin.archiveService().pruneAutomatedBackups(
                    this.plugin.automatedBackupRetentionCount(),
                    AUTOMATED_PREFIXES
            );
            this.plugin.diagnostics().writeBackupReport(
                    trigger,
                    backupFile,
                    pruned,
                    snapshot.getBalances().size(),
                    snapshot.getHistory().size(),
                    this.plugin.paymentPreferences().disabledPaymentCount(),
                    this.plugin.bankRegistry().bankCount()
            );
        } catch (Exception exception) {
            this.plugin.getLogger().warning("Coffers Legacy safety backup failed during " + trigger + ": " + exception.getMessage());
        }
    }
}
