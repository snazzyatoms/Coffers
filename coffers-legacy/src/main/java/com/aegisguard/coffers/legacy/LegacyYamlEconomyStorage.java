package com.aegisguard.coffers.legacy;

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

final class LegacyYamlEconomyStorage implements LegacyEconomyStorage {

    private final JavaPlugin plugin;
    private final File accountsFile;
    private final File historyFile;
    private final File metadataFile;
    private YamlConfiguration accountsConfig;
    private YamlConfiguration historyConfig;
    private YamlConfiguration metadataConfig;

    LegacyYamlEconomyStorage(final JavaPlugin plugin, final String accountsFileName, final String historyFileName) {
        this.plugin = plugin;
        this.accountsFile = new File(plugin.getDataFolder(), accountsFileName);
        this.historyFile = new File(plugin.getDataFolder(), historyFileName);
        this.metadataFile = new File(plugin.getDataFolder(), "storage-metadata.yml");
    }

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
        this.metadataConfig.set("storage.schema-version", Integer.valueOf(1));
        this.metadataConfig.save(this.metadataFile);
    }

    public LegacyStorageSnapshot load() {
        Map<UUID, Map<String, BigDecimal>> balances = new HashMap<UUID, Map<String, BigDecimal>>();
        ConfigurationSection accountsSection = this.accountsConfig.getConfigurationSection("accounts");
        if (accountsSection != null) {
            for (String accountKey : accountsSection.getKeys(false)) {
                UUID accountId = UUID.fromString(accountKey);
                ConfigurationSection balanceSection = accountsSection.getConfigurationSection(accountKey + ".balances");
                Map<String, BigDecimal> accountBalances = new HashMap<String, BigDecimal>();
                if (balanceSection != null) {
                    for (String currencyId : balanceSection.getKeys(false)) {
                        accountBalances.put(currencyId, new BigDecimal(balanceSection.getString(currencyId, "0")));
                    }
                }
                balances.put(accountId, accountBalances);
            }
        }

        Map<UUID, List<LegacyLedgerEntry>> history = new HashMap<UUID, List<LegacyLedgerEntry>>();
        ConfigurationSection historySection = this.historyConfig.getConfigurationSection("history");
        if (historySection != null) {
            for (String accountKey : historySection.getKeys(false)) {
                UUID accountId = UUID.fromString(accountKey);
                ConfigurationSection accountHistory = historySection.getConfigurationSection(accountKey);
                List<LegacyLedgerEntry> entries = new ArrayList<LegacyLedgerEntry>();
                if (accountHistory != null) {
                    for (String entryKey : accountHistory.getKeys(false)) {
                        ConfigurationSection entrySection = accountHistory.getConfigurationSection(entryKey);
                        if (entrySection != null) {
                            entries.add(readEntry(entrySection));
                        }
                    }
                }
                history.put(accountId, entries);
            }
        }

        return new LegacyStorageSnapshot(balances, history);
    }

    public void saveAccount(final UUID accountId, final Map<String, BigDecimal> balances) throws IOException {
        String path = "accounts." + accountId.toString() + ".balances";
        this.accountsConfig.set(path, null);
        for (Map.Entry<String, BigDecimal> entry : balances.entrySet()) {
            this.accountsConfig.set(path + "." + entry.getKey(), entry.getValue().toPlainString());
        }
        this.accountsConfig.save(this.accountsFile);
    }

    public void saveHistory(final UUID accountId, final List<LegacyLedgerEntry> entries) throws IOException {
        String path = "history." + accountId.toString();
        this.historyConfig.set(path, null);
        for (LegacyLedgerEntry entry : entries) {
            String entryPath = path + "." + entry.getEntryId().toString();
            writeEntry(entryPath, entry);
        }
        this.historyConfig.save(this.historyFile);
    }

    public void close() {
    }

    private void writeEntry(final String path, final LegacyLedgerEntry entry) {
        LegacyTransactionActor actor = entry.getActor() == null ? LegacyTransactionActor.system("yaml-storage") : entry.getActor();
        this.historyConfig.set(path + ".reference-id", entry.getReferenceId().toString());
        this.historyConfig.set(path + ".account-id", entry.getAccountId().toString());
        this.historyConfig.set(path + ".counterparty-account-id", entry.getCounterpartyAccountId() == null ? null : entry.getCounterpartyAccountId().toString());
        this.historyConfig.set(path + ".currency-id", entry.getCurrencyId());
        this.historyConfig.set(path + ".kind", entry.getKind().name());
        this.historyConfig.set(path + ".amount", entry.getAmount().toPlainString());
        this.historyConfig.set(path + ".resulting-balance", entry.getResultingBalance().toPlainString());
        this.historyConfig.set(path + ".actor.type", actor.getType().name());
        this.historyConfig.set(path + ".actor.id", actor.getActorId() == null ? null : actor.getActorId().toString());
        this.historyConfig.set(path + ".actor.name", actor.getActorName());
        this.historyConfig.set(path + ".actor.source", actor.getSource());
        this.historyConfig.set(path + ".reason", entry.getReason());
        this.historyConfig.set(path + ".created-at", Long.valueOf(entry.getCreatedAtEpochMilli()));
    }

    private LegacyLedgerEntry readEntry(final ConfigurationSection section) {
        ConfigurationSection actorSection = section.getConfigurationSection("actor");
        LegacyTransactionActor actor = actorSection == null
                ? LegacyTransactionActor.system("yaml-storage")
                : new LegacyTransactionActor(
                        LegacyTransactionActorType.valueOf(actorSection.getString("type", LegacyTransactionActorType.SYSTEM.name())),
                        actorSection.getString("id") == null ? null : UUID.fromString(actorSection.getString("id")),
                        actorSection.getString("name"),
                        actorSection.getString("source")
                );
        return new LegacyLedgerEntry(
                UUID.fromString(section.getName()),
                UUID.fromString(section.getString("reference-id")),
                UUID.fromString(section.getString("account-id")),
                section.getString("counterparty-account-id") == null ? null : UUID.fromString(section.getString("counterparty-account-id")),
                section.getString("currency-id"),
                LegacyTransactionKind.valueOf(section.getString("kind")),
                new BigDecimal(section.getString("amount", "0")),
                new BigDecimal(section.getString("resulting-balance", "0")),
                actor,
                section.getString("reason"),
                section.getLong("created-at")
        );
    }
}
