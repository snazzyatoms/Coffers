package com.aegisguard.coffers.paper;

import com.aegisguard.coffers.api.LedgerEntry;
import com.aegisguard.coffers.api.TransactionActor;
import com.aegisguard.coffers.api.TransactionActorType;
import com.aegisguard.coffers.api.TransactionKind;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

final class SnapshotArchiveService {

    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final JavaPlugin plugin;

    SnapshotArchiveService(final JavaPlugin plugin) {
        this.plugin = plugin;
    }

    File backup(final StorageSnapshot snapshot) throws IOException {
        final File backupDirectory = new File(this.plugin.getDataFolder(), "backups");
        if (!backupDirectory.exists() && !backupDirectory.mkdirs()) {
            throw new IOException("Could not create backups directory.");
        }
        return writeSnapshot(snapshot, new File(backupDirectory, "coffers-backup-" + FILE_STAMP.format(LocalDateTime.now()) + ".yml"));
    }

    File export(final String exportName, final StorageSnapshot snapshot) throws IOException {
        final File exportDirectory = new File(this.plugin.getDataFolder(), "exports");
        if (!exportDirectory.exists() && !exportDirectory.mkdirs()) {
            throw new IOException("Could not create exports directory.");
        }

        final String safeName = (exportName == null || exportName.isBlank())
                ? "coffers-export-" + FILE_STAMP.format(LocalDateTime.now())
                : exportName.replaceAll("[^a-zA-Z0-9._-]", "_");
        return writeSnapshot(snapshot, new File(exportDirectory, safeName + ".yml"));
    }

    StorageSnapshot importSnapshot(final String importName) throws IOException {
        final String safeName = importName.replaceAll("[^a-zA-Z0-9._-]", "_");
        final File file = new File(new File(this.plugin.getDataFolder(), "exports"), safeName.endsWith(".yml") ? safeName : safeName + ".yml");
        if (!file.exists()) {
            throw new IOException("Export file not found: " + file.getName());
        }
        return readSnapshot(file);
    }

    private File writeSnapshot(final StorageSnapshot snapshot, final File destination) throws IOException {
        final YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("metadata.schema-version", 1);
        configuration.set("metadata.created-at", System.currentTimeMillis());

        for (final Map.Entry<UUID, Map<String, BigDecimal>> entry : snapshot.balances().entrySet()) {
            final String accountPath = "balances." + entry.getKey();
            for (final Map.Entry<String, BigDecimal> balance : entry.getValue().entrySet()) {
                configuration.set(accountPath + "." + balance.getKey(), balance.getValue().toPlainString());
            }
        }

        for (final Map.Entry<UUID, List<LedgerEntry>> entry : snapshot.history().entrySet()) {
            final String accountPath = "history." + entry.getKey();
            for (final LedgerEntry ledgerEntry : entry.getValue()) {
                final String entryPath = accountPath + "." + ledgerEntry.entryId();
                configuration.set(entryPath + ".reference-id", ledgerEntry.referenceId().toString());
                configuration.set(entryPath + ".account-id", ledgerEntry.accountId().toString());
                configuration.set(entryPath + ".counterparty-account-id", ledgerEntry.counterpartyAccountId() == null ? null : ledgerEntry.counterpartyAccountId().toString());
                configuration.set(entryPath + ".currency-id", ledgerEntry.currencyId());
                configuration.set(entryPath + ".kind", ledgerEntry.kind().name());
                configuration.set(entryPath + ".amount", ledgerEntry.amount().toPlainString());
                configuration.set(entryPath + ".resulting-balance", ledgerEntry.resultingBalance().toPlainString());
                final TransactionActor actor = ledgerEntry.actor() == null ? TransactionActor.system("snapshot-export") : ledgerEntry.actor();
                configuration.set(entryPath + ".actor.type", actor.type().name());
                configuration.set(entryPath + ".actor.id", actor.actorId() == null ? null : actor.actorId().toString());
                configuration.set(entryPath + ".actor.name", actor.actorName());
                configuration.set(entryPath + ".actor.source", actor.source());
                configuration.set(entryPath + ".reason", ledgerEntry.reason());
                configuration.set(entryPath + ".created-at", ledgerEntry.createdAtEpochMilli());
            }
        }

        configuration.save(destination);
        return destination;
    }

    private StorageSnapshot readSnapshot(final File file) {
        final YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        final Map<UUID, Map<String, BigDecimal>> balances = new LinkedHashMap<>();
        final Map<UUID, List<LedgerEntry>> history = new LinkedHashMap<>();

        final ConfigurationSection balancesSection = configuration.getConfigurationSection("balances");
        if (balancesSection != null) {
            for (final String accountKey : balancesSection.getKeys(false)) {
                final ConfigurationSection accountSection = balancesSection.getConfigurationSection(accountKey);
                if (accountSection == null) {
                    continue;
                }

                final Map<String, BigDecimal> accountBalances = new LinkedHashMap<>();
                for (final String currencyId : accountSection.getKeys(false)) {
                    accountBalances.put(currencyId, new BigDecimal(accountSection.getString(currencyId, "0")));
                }
                balances.put(UUID.fromString(accountKey), accountBalances);
            }
        }

        final ConfigurationSection historySection = configuration.getConfigurationSection("history");
        if (historySection != null) {
            for (final String accountKey : historySection.getKeys(false)) {
                final ConfigurationSection accountSection = historySection.getConfigurationSection(accountKey);
                if (accountSection == null) {
                    continue;
                }

                final List<LedgerEntry> entries = new ArrayList<>();
                for (final String entryKey : accountSection.getKeys(false)) {
                    final ConfigurationSection entrySection = accountSection.getConfigurationSection(entryKey);
                    if (entrySection == null) {
                        continue;
                    }

                    final ConfigurationSection actorSection = entrySection.getConfigurationSection("actor");
                    final TransactionActor actor = actorSection == null
                            ? TransactionActor.system("snapshot-import")
                            : new TransactionActor(
                                    TransactionActorType.valueOf(actorSection.getString("type", TransactionActorType.SYSTEM.name())),
                                    actorSection.getString("id") == null ? null : UUID.fromString(actorSection.getString("id")),
                                    actorSection.getString("name"),
                                    actorSection.getString("source")
                            );
                    entries.add(new LedgerEntry(
                            UUID.fromString(entryKey),
                            UUID.fromString(entrySection.getString("reference-id")),
                            UUID.fromString(entrySection.getString("account-id")),
                            entrySection.getString("counterparty-account-id") == null ? null : UUID.fromString(entrySection.getString("counterparty-account-id")),
                            entrySection.getString("currency-id"),
                            TransactionKind.valueOf(entrySection.getString("kind")),
                            new BigDecimal(entrySection.getString("amount", "0")),
                            new BigDecimal(entrySection.getString("resulting-balance", "0")),
                            actor,
                            entrySection.getString("reason"),
                            entrySection.getLong("created-at")
                    ));
                }
                history.put(UUID.fromString(accountKey), entries);
            }
        }

        return new StorageSnapshot(balances, history);
    }
}
