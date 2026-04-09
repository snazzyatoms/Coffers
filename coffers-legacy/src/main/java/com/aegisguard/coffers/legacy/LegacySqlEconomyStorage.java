package com.aegisguard.coffers.legacy;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

final class LegacySqlEconomyStorage implements LegacyEconomyStorage {

    private final String jdbcUrl;
    private final Properties properties;

    LegacySqlEconomyStorage(final String jdbcUrl, final Properties properties) {
        this.jdbcUrl = jdbcUrl;
        this.properties = properties;
    }

    public void initialize() throws Exception {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = openConnection();
            statement = connection.createStatement();
            statement.execute("CREATE TABLE IF NOT EXISTS coffers_legacy_accounts (account_uuid VARCHAR(36) NOT NULL, currency_id VARCHAR(64) NOT NULL, balance_value VARCHAR(64) NOT NULL, PRIMARY KEY (account_uuid, currency_id))");
            statement.execute("CREATE TABLE IF NOT EXISTS coffers_legacy_history (entry_id VARCHAR(36) NOT NULL, account_uuid VARCHAR(36) NOT NULL, reference_id VARCHAR(36) NOT NULL, counterparty_uuid VARCHAR(36) NULL, currency_id VARCHAR(64) NOT NULL, transaction_kind VARCHAR(32) NOT NULL, amount_value VARCHAR(64) NOT NULL, resulting_balance VARCHAR(64) NOT NULL, actor_type VARCHAR(32) NOT NULL, actor_id VARCHAR(36) NULL, actor_name VARCHAR(128) NULL, actor_source VARCHAR(128) NULL, reason_value VARCHAR(255) NULL, created_at BIGINT NOT NULL, PRIMARY KEY (entry_id))");
        } finally {
            closeQuietly(statement);
            closeQuietly(connection);
        }
    }

    public LegacyStorageSnapshot load() throws Exception {
        Map<UUID, Map<String, BigDecimal>> balances = new HashMap<UUID, Map<String, BigDecimal>>();
        Map<UUID, List<LegacyLedgerEntry>> history = new HashMap<UUID, List<LegacyLedgerEntry>>();
        Connection connection = null;
        PreparedStatement accountStatement = null;
        PreparedStatement historyStatement = null;
        ResultSet accountResult = null;
        ResultSet historyResult = null;
        try {
            connection = openConnection();
            accountStatement = connection.prepareStatement("SELECT account_uuid, currency_id, balance_value FROM coffers_legacy_accounts");
            accountResult = accountStatement.executeQuery();
            while (accountResult.next()) {
                UUID accountId = UUID.fromString(accountResult.getString("account_uuid"));
                Map<String, BigDecimal> accountBalances = balances.get(accountId);
                if (accountBalances == null) {
                    accountBalances = new HashMap<String, BigDecimal>();
                    balances.put(accountId, accountBalances);
                }
                accountBalances.put(accountResult.getString("currency_id"), new BigDecimal(accountResult.getString("balance_value")));
            }

            historyStatement = connection.prepareStatement("SELECT * FROM coffers_legacy_history ORDER BY created_at ASC");
            historyResult = historyStatement.executeQuery();
            while (historyResult.next()) {
                LegacyLedgerEntry entry = new LegacyLedgerEntry(
                        UUID.fromString(historyResult.getString("entry_id")),
                        UUID.fromString(historyResult.getString("reference_id")),
                        UUID.fromString(historyResult.getString("account_uuid")),
                        historyResult.getString("counterparty_uuid") == null ? null : UUID.fromString(historyResult.getString("counterparty_uuid")),
                        historyResult.getString("currency_id"),
                        LegacyTransactionKind.valueOf(historyResult.getString("transaction_kind")),
                        new BigDecimal(historyResult.getString("amount_value")),
                        new BigDecimal(historyResult.getString("resulting_balance")),
                        new LegacyTransactionActor(
                                LegacyTransactionActorType.valueOf(historyResult.getString("actor_type")),
                                historyResult.getString("actor_id") == null ? null : UUID.fromString(historyResult.getString("actor_id")),
                                historyResult.getString("actor_name"),
                                historyResult.getString("actor_source")
                        ),
                        historyResult.getString("reason_value"),
                        historyResult.getLong("created_at")
                );
                List<LegacyLedgerEntry> entries = history.get(entry.getAccountId());
                if (entries == null) {
                    entries = new ArrayList<LegacyLedgerEntry>();
                    history.put(entry.getAccountId(), entries);
                }
                entries.add(entry);
            }
        } finally {
            closeQuietly(accountResult);
            closeQuietly(historyResult);
            closeQuietly(accountStatement);
            closeQuietly(historyStatement);
            closeQuietly(connection);
        }
        return new LegacyStorageSnapshot(balances, history);
    }

    public void saveAccount(final UUID accountId, final Map<String, BigDecimal> balances) throws Exception {
        Connection connection = null;
        PreparedStatement delete = null;
        PreparedStatement insert = null;
        try {
            connection = openConnection();
            delete = connection.prepareStatement("DELETE FROM coffers_legacy_accounts WHERE account_uuid = ?");
            delete.setString(1, accountId.toString());
            delete.executeUpdate();

            insert = connection.prepareStatement("INSERT INTO coffers_legacy_accounts (account_uuid, currency_id, balance_value) VALUES (?, ?, ?)");
            for (Map.Entry<String, BigDecimal> entry : balances.entrySet()) {
                insert.setString(1, accountId.toString());
                insert.setString(2, entry.getKey());
                insert.setString(3, entry.getValue().toPlainString());
                insert.addBatch();
            }
            insert.executeBatch();
        } finally {
            closeQuietly(delete);
            closeQuietly(insert);
            closeQuietly(connection);
        }
    }

    public void saveHistory(final UUID accountId, final List<LegacyLedgerEntry> entries) throws Exception {
        Connection connection = null;
        PreparedStatement delete = null;
        PreparedStatement insert = null;
        try {
            connection = openConnection();
            delete = connection.prepareStatement("DELETE FROM coffers_legacy_history WHERE account_uuid = ?");
            delete.setString(1, accountId.toString());
            delete.executeUpdate();

            insert = connection.prepareStatement("INSERT INTO coffers_legacy_history (entry_id, account_uuid, reference_id, counterparty_uuid, currency_id, transaction_kind, amount_value, resulting_balance, actor_type, actor_id, actor_name, actor_source, reason_value, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            for (LegacyLedgerEntry entry : entries) {
                insert.setString(1, entry.getEntryId().toString());
                insert.setString(2, entry.getAccountId().toString());
                insert.setString(3, entry.getReferenceId().toString());
                insert.setString(4, entry.getCounterpartyAccountId() == null ? null : entry.getCounterpartyAccountId().toString());
                insert.setString(5, entry.getCurrencyId());
                insert.setString(6, entry.getKind().name());
                insert.setString(7, entry.getAmount().toPlainString());
                insert.setString(8, entry.getResultingBalance().toPlainString());
                insert.setString(9, entry.getActor().getType().name());
                insert.setString(10, entry.getActor().getActorId() == null ? null : entry.getActor().getActorId().toString());
                insert.setString(11, entry.getActor().getActorName());
                insert.setString(12, entry.getActor().getSource());
                insert.setString(13, entry.getReason());
                insert.setLong(14, entry.getCreatedAtEpochMilli());
                insert.addBatch();
            }
            insert.executeBatch();
        } finally {
            closeQuietly(delete);
            closeQuietly(insert);
            closeQuietly(connection);
        }
    }

    public void close() {
    }

    private Connection openConnection() throws Exception {
        if (this.properties.isEmpty()) {
            return DriverManager.getConnection(this.jdbcUrl);
        }
        return DriverManager.getConnection(this.jdbcUrl, this.properties);
    }

    private void closeQuietly(final AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (final Exception ignored) {
            }
        }
    }
}
