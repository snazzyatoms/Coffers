package com.aegisguard.coffers.legacy;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

final class LegacySnapshotArchiveService {

    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final File dataFolder;

    LegacySnapshotArchiveService(final JavaPlugin plugin) {
        this(plugin.getDataFolder());
    }

    LegacySnapshotArchiveService(final File dataFolder) {
        this.dataFolder = dataFolder;
    }

    File backup(final String backupName, final LegacyStorageSnapshot snapshot, final Set<UUID> disabledPaymentAccounts, final Set<String> banks) throws IOException {
        File backupDirectory = backupDirectory();
        if (!backupDirectory.exists() && !backupDirectory.mkdirs()) {
            throw new IOException("Could not create backups directory.");
        }
        String safeName = safeName(backupName, "coffers-legacy-backup-" + FILE_STAMP.format(LocalDateTime.now()));
        return writeSnapshot(snapshot, disabledPaymentAccounts, banks, new File(backupDirectory, safeName + ".yml"));
    }

    File export(final String exportName, final LegacyStorageSnapshot snapshot, final Set<UUID> disabledPaymentAccounts, final Set<String> banks) throws IOException {
        File exportDirectory = exportDirectory();
        if (!exportDirectory.exists() && !exportDirectory.mkdirs()) {
            throw new IOException("Could not create exports directory.");
        }
        String safeName = safeName(exportName, "coffers-legacy-export-" + FILE_STAMP.format(LocalDateTime.now()));
        return writeSnapshot(snapshot, disabledPaymentAccounts, banks, new File(exportDirectory, safeName + ".yml"));
    }

    LegacyArchiveSnapshot importSnapshot(final String importName) throws IOException {
        String safeName = importName.replaceAll("[^a-zA-Z0-9._-]", "_");
        File file = new File(exportDirectory(), safeName.endsWith(".yml") ? safeName : safeName + ".yml");
        if (!file.exists()) {
            throw new IOException("Export file not found: " + file.getName());
        }
        return readSnapshot(file);
    }

    LegacyArchiveSnapshot restoreBackup(final String backupName) throws IOException {
        File backupFile;
        if (backupName == null || backupName.trim().isEmpty() || "latest".equalsIgnoreCase(backupName)) {
            backupFile = latestBackupFile();
            if (backupFile == null) {
                throw new IOException("No Coffers Legacy backups were found.");
            }
        } else {
            String safeName = backupName.replaceAll("[^a-zA-Z0-9._-]", "_");
            backupFile = new File(backupDirectory(), safeName.endsWith(".yml") ? safeName : safeName + ".yml");
            if (!backupFile.exists()) {
                throw new IOException("Backup file not found: " + backupFile.getName());
            }
        }
        return readSnapshot(backupFile);
    }

    private File latestBackupFile() {
        File[] files = backupDirectory().listFiles();
        if (files == null || files.length == 0) {
            return null;
        }

        File newest = null;
        for (File file : files) {
            if (!file.getName().endsWith(".yml")) {
                continue;
            }
            if (newest == null || file.lastModified() > newest.lastModified()) {
                newest = file;
            }
        }
        return newest;
    }

    private File backupDirectory() {
        return new File(this.dataFolder, "backups");
    }

    private File exportDirectory() {
        return new File(this.dataFolder, "exports");
    }

    private String safeName(final String requestedName, final String fallbackName) {
        if (requestedName == null || requestedName.trim().isEmpty()) {
            return fallbackName;
        }
        return requestedName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private File writeSnapshot(
            final LegacyStorageSnapshot snapshot,
            final Set<UUID> disabledPaymentAccounts,
            final Set<String> banks,
            final File destination
    ) throws IOException {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("metadata.schema-version", Integer.valueOf(1));
        configuration.set("metadata.created-at", Long.valueOf(System.currentTimeMillis()));
        for (UUID accountId : disabledPaymentAccounts) {
            configuration.set("preferences.disabled-payments." + accountId.toString(), Boolean.TRUE);
        }
        configuration.set("banks", new ArrayList<String>(banks));

        for (Map.Entry<UUID, Map<String, BigDecimal>> entry : snapshot.getBalances().entrySet()) {
            String accountPath = "balances." + entry.getKey().toString();
            for (Map.Entry<String, BigDecimal> balance : entry.getValue().entrySet()) {
                configuration.set(accountPath + "." + balance.getKey(), balance.getValue().toPlainString());
            }
        }

        for (Map.Entry<UUID, List<LegacyLedgerEntry>> entry : snapshot.getHistory().entrySet()) {
            String accountPath = "history." + entry.getKey().toString();
            for (LegacyLedgerEntry ledgerEntry : entry.getValue()) {
                String entryPath = accountPath + "." + ledgerEntry.getEntryId().toString();
                configuration.set(entryPath + ".reference-id", ledgerEntry.getReferenceId().toString());
                configuration.set(entryPath + ".account-id", ledgerEntry.getAccountId().toString());
                configuration.set(entryPath + ".counterparty-account-id", ledgerEntry.getCounterpartyAccountId() == null ? null : ledgerEntry.getCounterpartyAccountId().toString());
                configuration.set(entryPath + ".currency-id", ledgerEntry.getCurrencyId());
                configuration.set(entryPath + ".kind", ledgerEntry.getKind().name());
                configuration.set(entryPath + ".amount", ledgerEntry.getAmount().toPlainString());
                configuration.set(entryPath + ".previous-balance", ledgerEntry.getPreviousBalance().toPlainString());
                configuration.set(entryPath + ".resulting-balance", ledgerEntry.getResultingBalance().toPlainString());
                LegacyTransactionActor actor = ledgerEntry.getActor() == null ? LegacyTransactionActor.system("snapshot-export") : ledgerEntry.getActor();
                configuration.set(entryPath + ".actor.type", actor.getType().name());
                configuration.set(entryPath + ".actor.id", actor.getActorId() == null ? null : actor.getActorId().toString());
                configuration.set(entryPath + ".actor.name", actor.getActorName());
                configuration.set(entryPath + ".actor.source", actor.getSource());
                configuration.set(entryPath + ".reason", ledgerEntry.getReason());
                configuration.set(entryPath + ".reversal-of-reference-id", ledgerEntry.getReversalOfReferenceId() == null ? null : ledgerEntry.getReversalOfReferenceId().toString());
                configuration.set(entryPath + ".created-at", Long.valueOf(ledgerEntry.getCreatedAtEpochMilli()));
            }
        }

        configuration.save(destination);
        return destination;
    }

    private LegacyArchiveSnapshot readSnapshot(final File file) {
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        Map<UUID, Map<String, BigDecimal>> balances = new LinkedHashMap<UUID, Map<String, BigDecimal>>();
        Map<UUID, List<LegacyLedgerEntry>> history = new LinkedHashMap<UUID, List<LegacyLedgerEntry>>();
        Set<UUID> disabledPaymentAccounts = new LinkedHashSet<UUID>();
        Set<String> banks = new LinkedHashSet<String>();

        ConfigurationSection balancesSection = configuration.getConfigurationSection("balances");
        if (balancesSection != null) {
            for (String accountKey : balancesSection.getKeys(false)) {
                ConfigurationSection accountSection = balancesSection.getConfigurationSection(accountKey);
                if (accountSection == null) {
                    continue;
                }

                Map<String, BigDecimal> accountBalances = new LinkedHashMap<String, BigDecimal>();
                for (String currencyId : accountSection.getKeys(false)) {
                    accountBalances.put(currencyId, new BigDecimal(accountSection.getString(currencyId, "0")));
                }
                balances.put(UUID.fromString(accountKey), accountBalances);
            }
        }

        ConfigurationSection historySection = configuration.getConfigurationSection("history");
        if (historySection != null) {
            for (String accountKey : historySection.getKeys(false)) {
                ConfigurationSection accountSection = historySection.getConfigurationSection(accountKey);
                if (accountSection == null) {
                    continue;
                }

                List<LegacyLedgerEntry> entries = new ArrayList<LegacyLedgerEntry>();
                for (String entryKey : accountSection.getKeys(false)) {
                    ConfigurationSection entrySection = accountSection.getConfigurationSection(entryKey);
                    if (entrySection == null) {
                        continue;
                    }

                    ConfigurationSection actorSection = entrySection.getConfigurationSection("actor");
                    LegacyTransactionActor actor = actorSection == null
                            ? LegacyTransactionActor.system("snapshot-import")
                            : new LegacyTransactionActor(
                                    LegacyTransactionActorType.valueOf(actorSection.getString("type", LegacyTransactionActorType.SYSTEM.name())),
                                    actorSection.getString("id") == null ? null : UUID.fromString(actorSection.getString("id")),
                                    actorSection.getString("name"),
                                    actorSection.getString("source")
                            );
                    entries.add(new LegacyLedgerEntry(
                            UUID.fromString(entryKey),
                            UUID.fromString(entrySection.getString("reference-id")),
                            UUID.fromString(entrySection.getString("account-id")),
                            entrySection.getString("counterparty-account-id") == null ? null : UUID.fromString(entrySection.getString("counterparty-account-id")),
                            entrySection.getString("currency-id"),
                            LegacyTransactionKind.valueOf(entrySection.getString("kind")),
                            new BigDecimal(entrySection.getString("amount", "0")),
                            readPreviousBalance(entrySection),
                            new BigDecimal(entrySection.getString("resulting-balance", "0")),
                            actor,
                            entrySection.getString("reason"),
                            entrySection.getString("reversal-of-reference-id") == null ? null : UUID.fromString(entrySection.getString("reversal-of-reference-id")),
                            entrySection.getLong("created-at")
                    ));
                }
                history.put(UUID.fromString(accountKey), entries);
            }
        }

        ConfigurationSection preferencesSection = configuration.getConfigurationSection("preferences.disabled-payments");
        if (preferencesSection != null) {
            for (String accountKey : preferencesSection.getKeys(false)) {
                try {
                    if (preferencesSection.getBoolean(accountKey, false)) {
                        disabledPaymentAccounts.add(UUID.fromString(accountKey));
                    }
                } catch (IllegalArgumentException ignored) {
                    // Ignore malformed UUIDs in imported preference data.
                }
            }
        }

        List<?> storedBanks = configuration.getList("banks");
        if (storedBanks != null) {
            for (Object value : storedBanks) {
                if (value != null && !value.toString().trim().isEmpty()) {
                    banks.add(value.toString());
                }
            }
        }

        return new LegacyArchiveSnapshot(new LegacyStorageSnapshot(balances, history), disabledPaymentAccounts, banks);
    }

    private BigDecimal readPreviousBalance(final ConfigurationSection entrySection) {
        if (entrySection.getString("previous-balance") != null) {
            return new BigDecimal(entrySection.getString("previous-balance"));
        }

        BigDecimal amount = new BigDecimal(entrySection.getString("amount", "0"));
        BigDecimal resultingBalance = new BigDecimal(entrySection.getString("resulting-balance", "0"));
        LegacyTransactionKind kind = LegacyTransactionKind.valueOf(entrySection.getString("kind"));
        switch (kind) {
            case DEPOSIT:
            case TRANSFER_IN:
                return resultingBalance.subtract(amount);
            case WITHDRAWAL:
            case TRANSFER_OUT:
                return resultingBalance.add(amount);
            case SET:
            default:
                return resultingBalance;
        }
    }
}
