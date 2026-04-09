package com.aegisguard.coffers.paper;

import com.aegisguard.coffers.api.LedgerEntry;
import com.aegisguard.coffers.api.TransactionActor;
import com.aegisguard.coffers.api.TransactionActorType;
import com.aegisguard.coffers.api.TransactionKind;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

final class YamlEconomyStorage implements EconomyStorage {

    private final JavaPlugin plugin;
    private final File accountsFile;
    private final File historyFile;
    private final File metadataFile;
    private YamlConfiguration accountsConfig;
    private YamlConfiguration historyConfig;
    private YamlConfiguration metadataConfig;

    YamlEconomyStorage(final JavaPlugin plugin, final String accountsFileName, final String historyFileName) {
        this.plugin = plugin;
        this.accountsFile = new File(plugin.getDataFolder(), accountsFileName);
        this.historyFile = new File(plugin.getDataFolder(), historyFileName);
        this.metadataFile = new File(plugin.getDataFolder(), "storage-metadata.yml");
    }

    @Override
    public void initialize() throws IOException {
        if (!this.plugin.getDataFolder().exists() && !this.plugin.getDataFolder().mkdirs()) {
            throw new IOException("Could not create plugin data folder.");
        }

        if (!this.accountsFile.exists() && !this.accountsFile.createNewFile()) {
            throw new IOException("Could not create accounts file: " + this.accountsFile.getName());
        }

        if (!this.historyFile.exists() && !this.historyFile.createNewFile()) {
            throw new IOException("Could not create history file: " + this.historyFile.getName());
        }

        if (!this.metadataFile.exists() && !this.metadataFile.createNewFile()) {
            throw new IOException("Could not create metadata file: " + this.metadataFile.getName());
        }

        this.accountsConfig = YamlConfiguration.loadConfiguration(this.accountsFile);
        this.historyConfig = YamlConfiguration.loadConfiguration(this.historyFile);
        this.metadataConfig = YamlConfiguration.loadConfiguration(this.metadataFile);
        this.metadataConfig.set("storage.engine", "yaml");
        this.metadataConfig.set("storage.schema-version", 1);
        this.metadataConfig.save(this.metadataFile);
    }

    @Override
    public StorageSnapshot load() {
        final Map<UUID, Map<String, BigDecimal>> balances = new HashMap<>();
        final ConfigurationSection accountsSection = this.accountsConfig.getConfigurationSection("accounts");
        if (accountsSection != null) {
            for (final String accountKey : accountsSection.getKeys(false)) {
                final UUID accountId = UUID.fromString(accountKey);
                final ConfigurationSection balanceSection = accountsSection.getConfigurationSection(accountKey + ".balances");
                final Map<String, BigDecimal> accountBalances = new HashMap<>();
                if (balanceSection != null) {
                    for (final String currencyId : balanceSection.getKeys(false)) {
                        accountBalances.put(currencyId, new BigDecimal(balanceSection.getString(currencyId, "0")));
                    }
                }
                balances.put(accountId, accountBalances);
            }
        }

        final Map<UUID, List<LedgerEntry>> history = new HashMap<>();
        final ConfigurationSection historySection = this.historyConfig.getConfigurationSection("history");
        if (historySection != null) {
            for (final String accountKey : historySection.getKeys(false)) {
                final UUID accountId = UUID.fromString(accountKey);
                final ConfigurationSection accountHistory = historySection.getConfigurationSection(accountKey);
                final List<LedgerEntry> entries = new ArrayList<>();
                if (accountHistory != null) {
                    for (final String entryKey : accountHistory.getKeys(false)) {
                        final ConfigurationSection entrySection = accountHistory.getConfigurationSection(entryKey);
                        if (entrySection != null) {
                            entries.add(readEntry(entrySection));
                        }
                    }
                }
                history.put(accountId, entries);
            }
        }

        return new StorageSnapshot(balances, history);
    }

    @Override
    public void saveAccount(final UUID accountId, final Map<String, BigDecimal> balances) throws IOException {
        final String path = "accounts." + accountId + ".balances";
        this.accountsConfig.set(path, null);
        for (final Map.Entry<String, BigDecimal> entry : balances.entrySet()) {
            this.accountsConfig.set(path + "." + entry.getKey(), entry.getValue().toPlainString());
        }
        this.accountsConfig.save(this.accountsFile);
    }

    @Override
    public void saveHistory(final UUID accountId, final List<LedgerEntry> entries) throws IOException {
        final String path = "history." + accountId;
        this.historyConfig.set(path, null);
        for (final LedgerEntry entry : entries) {
            final String entryPath = path + "." + entry.entryId();
            writeEntry(entryPath, entry);
        }
        this.historyConfig.save(this.historyFile);
    }

    @Override
    public void close() {
        // No persistent resources to close for YAML-backed storage.
    }

    private void writeEntry(final String path, final LedgerEntry entry) {
        final TransactionActor actor = entry.actor() == null ? TransactionActor.system("yaml-storage") : entry.actor();
        this.historyConfig.set(path + ".reference-id", entry.referenceId().toString());
        this.historyConfig.set(path + ".account-id", entry.accountId().toString());
        this.historyConfig.set(path + ".counterparty-account-id", entry.counterpartyAccountId() == null ? null : entry.counterpartyAccountId().toString());
        this.historyConfig.set(path + ".currency-id", entry.currencyId());
        this.historyConfig.set(path + ".kind", entry.kind().name());
        this.historyConfig.set(path + ".amount", entry.amount().toPlainString());
        this.historyConfig.set(path + ".resulting-balance", entry.resultingBalance().toPlainString());
        this.historyConfig.set(path + ".actor.type", actor.type().name());
        this.historyConfig.set(path + ".actor.id", actor.actorId() == null ? null : actor.actorId().toString());
        this.historyConfig.set(path + ".actor.name", actor.actorName());
        this.historyConfig.set(path + ".actor.source", actor.source());
        this.historyConfig.set(path + ".reason", entry.reason());
        this.historyConfig.set(path + ".created-at", entry.createdAtEpochMilli());
    }

    private LedgerEntry readEntry(final ConfigurationSection section) {
        final ConfigurationSection actorSection = section.getConfigurationSection("actor");
        final TransactionActor actor = actorSection == null
                ? TransactionActor.system("yaml-storage")
                : new TransactionActor(
                        TransactionActorType.valueOf(actorSection.getString("type", TransactionActorType.SYSTEM.name())),
                        actorSection.getString("id") == null ? null : UUID.fromString(actorSection.getString("id")),
                        actorSection.getString("name"),
                        actorSection.getString("source")
                );
        return new LedgerEntry(
                UUID.fromString(section.getName()),
                UUID.fromString(section.getString("reference-id")),
                UUID.fromString(section.getString("account-id")),
                section.getString("counterparty-account-id") == null ? null : UUID.fromString(section.getString("counterparty-account-id")),
                section.getString("currency-id"),
                TransactionKind.valueOf(section.getString("kind")),
                new BigDecimal(section.getString("amount", "0")),
                new BigDecimal(section.getString("resulting-balance", "0")),
                actor,
                section.getString("reason"),
                section.getLong("created-at")
        );
    }
}
