package com.aegisguard.coffers.paper;

import com.aegisguard.coffers.api.LedgerEntry;
import com.aegisguard.coffers.api.TransactionActor;
import com.aegisguard.coffers.api.TransactionActorType;
import com.aegisguard.coffers.api.TransactionKind;
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

final class SqlEconomyStorage implements EconomyStorage {

    private final String jdbcUrl;
    private final Properties properties;

    SqlEconomyStorage(final String jdbcUrl, final Properties properties) {
        this.jdbcUrl = jdbcUrl;
        this.properties = properties;
    }

    @Override
    public void initialize() throws Exception {
        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS coffers_accounts (
                        account_uuid VARCHAR(36) NOT NULL,
                        currency_id VARCHAR(64) NOT NULL,
                        balance_value VARCHAR(64) NOT NULL,
                        PRIMARY KEY (account_uuid, currency_id)
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS coffers_history (
                        entry_id VARCHAR(36) NOT NULL,
                        account_uuid VARCHAR(36) NOT NULL,
                        reference_id VARCHAR(36) NOT NULL,
                        counterparty_uuid VARCHAR(36) NULL,
                        currency_id VARCHAR(64) NOT NULL,
                        transaction_kind VARCHAR(32) NOT NULL,
                        amount_value VARCHAR(64) NOT NULL,
                        resulting_balance VARCHAR(64) NOT NULL,
                        actor_type VARCHAR(32) NOT NULL,
                        actor_id VARCHAR(36) NULL,
                        actor_name VARCHAR(128) NULL,
                        actor_source VARCHAR(128) NULL,
                        reason_value VARCHAR(255) NULL,
                        created_at BIGINT NOT NULL,
                        PRIMARY KEY (entry_id)
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS coffers_metadata (
                        metadata_key VARCHAR(64) NOT NULL,
                        metadata_value VARCHAR(255) NOT NULL,
                        PRIMARY KEY (metadata_key)
                    )
                    """);
            statement.executeUpdate("DELETE FROM coffers_metadata WHERE metadata_key IN ('storage_engine', 'schema_version')");
            statement.executeUpdate("INSERT INTO coffers_metadata (metadata_key, metadata_value) VALUES ('storage_engine', 'sql')");
            statement.executeUpdate("INSERT INTO coffers_metadata (metadata_key, metadata_value) VALUES ('schema_version', '1')");
        }
    }

    @Override
    public StorageSnapshot load() throws Exception {
        final Map<UUID, Map<String, BigDecimal>> balances = new HashMap<>();
        final Map<UUID, List<LedgerEntry>> history = new HashMap<>();

        try (Connection connection = openConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT account_uuid, currency_id, balance_value FROM coffers_accounts"
            ); ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    final UUID accountId = UUID.fromString(resultSet.getString("account_uuid"));
                    final Map<String, BigDecimal> accountBalances = balances.computeIfAbsent(accountId, ignored -> new HashMap<>());
                    accountBalances.put(resultSet.getString("currency_id"), new BigDecimal(resultSet.getString("balance_value")));
                }
            }

            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM coffers_history ORDER BY created_at ASC"
            ); ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    final String actorType = resultSet.getString("actor_type");
                    final LedgerEntry entry = new LedgerEntry(
                            UUID.fromString(resultSet.getString("entry_id")),
                            UUID.fromString(resultSet.getString("reference_id")),
                            UUID.fromString(resultSet.getString("account_uuid")),
                            resultSet.getString("counterparty_uuid") == null ? null : UUID.fromString(resultSet.getString("counterparty_uuid")),
                            resultSet.getString("currency_id"),
                            TransactionKind.valueOf(resultSet.getString("transaction_kind")),
                            new BigDecimal(resultSet.getString("amount_value")),
                            new BigDecimal(resultSet.getString("resulting_balance")),
                            actorType == null
                                    ? TransactionActor.system("sql-storage")
                                    : new TransactionActor(
                                            TransactionActorType.valueOf(actorType),
                                            resultSet.getString("actor_id") == null ? null : UUID.fromString(resultSet.getString("actor_id")),
                                            resultSet.getString("actor_name"),
                                            resultSet.getString("actor_source")
                                    ),
                            resultSet.getString("reason_value"),
                            resultSet.getLong("created_at")
                    );
                    history.computeIfAbsent(entry.accountId(), ignored -> new ArrayList<>()).add(entry);
                }
            }
        }

        return new StorageSnapshot(balances, history);
    }

    @Override
    public void saveAccount(final UUID accountId, final Map<String, BigDecimal> balances) throws Exception {
        try (Connection connection = openConnection()) {
            try (PreparedStatement delete = connection.prepareStatement("DELETE FROM coffers_accounts WHERE account_uuid = ?")) {
                delete.setString(1, accountId.toString());
                delete.executeUpdate();
            }

            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO coffers_accounts (account_uuid, currency_id, balance_value) VALUES (?, ?, ?)"
            )) {
                for (final Map.Entry<String, BigDecimal> entry : balances.entrySet()) {
                    insert.setString(1, accountId.toString());
                    insert.setString(2, entry.getKey());
                    insert.setString(3, entry.getValue().toPlainString());
                    insert.addBatch();
                }
                insert.executeBatch();
            }
        }
    }

    @Override
    public void saveHistory(final UUID accountId, final List<LedgerEntry> entries) throws Exception {
        try (Connection connection = openConnection()) {
            try (PreparedStatement delete = connection.prepareStatement("DELETE FROM coffers_history WHERE account_uuid = ?")) {
                delete.setString(1, accountId.toString());
                delete.executeUpdate();
            }

            try (PreparedStatement insert = connection.prepareStatement("""
                    INSERT INTO coffers_history (
                        entry_id, account_uuid, reference_id, counterparty_uuid, currency_id, transaction_kind,
                        amount_value, resulting_balance, actor_type, actor_id, actor_name, actor_source,
                        reason_value, created_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """)) {
                for (final LedgerEntry entry : entries) {
                    final TransactionActor actor = entry.actor() == null ? TransactionActor.system("sql-storage") : entry.actor();
                    insert.setString(1, entry.entryId().toString());
                    insert.setString(2, entry.accountId().toString());
                    insert.setString(3, entry.referenceId().toString());
                    insert.setString(4, entry.counterpartyAccountId() == null ? null : entry.counterpartyAccountId().toString());
                    insert.setString(5, entry.currencyId());
                    insert.setString(6, entry.kind().name());
                    insert.setString(7, entry.amount().toPlainString());
                    insert.setString(8, entry.resultingBalance().toPlainString());
                    insert.setString(9, actor.type().name());
                    insert.setString(10, actor.actorId() == null ? null : actor.actorId().toString());
                    insert.setString(11, actor.actorName());
                    insert.setString(12, actor.source());
                    insert.setString(13, entry.reason());
                    insert.setLong(14, entry.createdAtEpochMilli());
                    insert.addBatch();
                }
                insert.executeBatch();
            }
        }
    }

    @Override
    public void close() {
        // Connections are opened per operation for this first implementation.
    }

    private Connection openConnection() throws Exception {
        return this.properties.isEmpty()
                ? DriverManager.getConnection(this.jdbcUrl)
                : DriverManager.getConnection(this.jdbcUrl, this.properties);
    }
}
