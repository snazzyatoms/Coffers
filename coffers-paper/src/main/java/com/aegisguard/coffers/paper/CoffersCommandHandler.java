package com.aegisguard.coffers.paper;

import com.aegisguard.coffers.api.AccountSnapshot;
import com.aegisguard.coffers.api.CoffersEconomy;
import com.aegisguard.coffers.api.LedgerEntry;
import com.aegisguard.coffers.api.TransactionActor;
import com.aegisguard.coffers.api.TransactionKind;
import com.aegisguard.coffers.api.TransactionResult;
import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

final class CoffersCommandHandler implements CommandExecutor, TabCompleter {

    private final CoffersPlugin plugin;

    CoffersCommandHandler(final CoffersPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        if (this.plugin.economy() == null) {
            messages().send(sender, "<error>Coffers is not fully loaded yet.");
            return true;
        }

        final String commandName = command.getName().toLowerCase(Locale.ROOT);
        if ("baltop".equals(commandName)) {
            return handleTop(sender, args, 0);
        }
        if ("paytoggle".equals(commandName)) {
            return handlePayToggle(sender, args, 0);
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        try {
            final String action = args[0].toLowerCase(Locale.ROOT);
            if ("balance".equals(action)) {
                return handleBalance(sender, args);
            }
            if ("pay".equals(action)) {
                return handlePay(sender, args);
            }
            if ("paytoggle".equals(action)) {
                return handlePayToggle(sender, args, 1);
            }
            if ("set".equals(action)) {
                return handleSet(sender, args);
            }
            if ("history".equals(action) || "transactionhistory".equals(action)) {
                return handleHistory(sender, args);
            }
            if ("rollback".equals(action)) {
                return handleRollback(sender, args);
            }
            if ("bank".equals(action) || "banks".equals(action)) {
                return handleBank(sender, args);
            }
            if ("currencies".equals(action)) {
                return handleCurrencies(sender);
            }
            if ("migratevault".equals(action)) {
                return handleMigrateVault(sender, args);
            }
            if ("reload".equals(action)) {
                return handleReload(sender);
            }
            if ("status".equals(action) || "info".equals(action)) {
                return handleStatus(sender);
            }
            if ("top".equals(action) || "baltop".equals(action)) {
                return handleTop(sender, args, 1);
            }
            if ("backup".equals(action)) {
                return handleBackup(sender, args);
            }
            if ("export".equals(action)) {
                return handleExport(sender, args);
            }
            if ("import".equals(action)) {
                return handleImport(sender, args);
            }
            if ("restore".equals(action) || "restorebackup".equals(action)) {
                return handleRestore(sender, args);
            }
            if ("restorepreview".equals(action) || "previewrestore".equals(action)) {
                return handleRestorePreview(sender, args);
            }
            if ("restoreaccount".equals(action)) {
                return handleRestoreAccount(sender, args);
            }
            if ("validate".equals(action) || "audit".equals(action)) {
                return handleValidate(sender);
            }
        } catch (final IllegalArgumentException exception) {
            messages().send(sender, "<error>Coffers error<secondary>: <primary>%message%", Map.of("message", exception.getMessage()));
            return true;
        } catch (final IllegalStateException exception) {
            messages().send(sender, "<error>Coffers failed to complete that request<secondary>: <primary>%message%", Map.of("message", exception.getMessage()));
            return true;
        }

        sendUsage(sender);
        return true;
    }

    private boolean handleBalance(final CommandSender sender, final String[] args) {
        if (args.length == 1) {
            if (!(sender instanceof Player player)) {
                messages().send(sender, "<error>Console must specify a player.");
                return true;
            }

            final String currencyId = economy().defaultCurrencyId();
            final BigDecimal balance = economy().getBalance(player.getUniqueId(), currencyId);
            messages().send(sender, "<success>Balance<secondary>: <highlight>%balance%", Map.of("balance", economy().format(currencyId, balance)));
            return true;
        }

        if (args.length == 2 && sender instanceof Player player && economy().currency(args[1]).isPresent()) {
            final String currencyId = args[1];
            final BigDecimal balance = economy().getBalance(player.getUniqueId(), currencyId);
            messages().send(sender, "<success>Balance<secondary>: <highlight>%balance%", Map.of("balance", economy().format(currencyId, balance)));
            return true;
        }

        if (!sender.hasPermission("coffers.command.balance.others")) {
            messages().send(sender, "<error>You do not have permission to view another player's balance.");
            return true;
        }

        final OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        final String currencyId = args.length >= 3 ? args[2] : economy().defaultCurrencyId();
        final BigDecimal balance = economy().getBalance(target.getUniqueId(), currencyId);
        messages().send(
                sender,
                "<highlight>%player%<secondary> has <highlight>%balance%",
                Map.of("player", displayName(target), "balance", economy().format(currencyId, balance))
        );
        return true;
    }

    private boolean handlePay(final CommandSender sender, final String[] args) {
        if (!(sender instanceof Player player)) {
            messages().send(sender, "<error>Only players can use /coffers pay.");
            return true;
        }

        if (args.length < 3) {
            messages().send(sender, "<usage>Usage<secondary>: <primary>/coffers pay <player> <amount> [currency]");
            return true;
        }

        final OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (Objects.equals(target.getUniqueId(), player.getUniqueId())) {
            messages().send(sender, "<error>You cannot pay yourself.");
            return true;
        }

        if (!this.plugin.paymentPreferences().allowsPayments(target.getUniqueId())
                && !sender.hasPermission("coffers.command.pay.ignore-toggle")) {
            messages().send(sender, "<warning>%player% is not accepting Coffers payments right now.", Map.of("player", displayName(target)));
            return true;
        }

        final BigDecimal amount = parseAmount(sender, args[2]);
        if (amount == null) {
            return true;
        }
        final String currencyId = args.length >= 4 ? args[3] : economy().defaultCurrencyId();

        final TransactionResult result = economy().transfer(
                player.getUniqueId(),
                target.getUniqueId(),
                currencyId,
                amount,
                TransactionActor.player(player.getUniqueId(), player.getName(), "command:/coffers pay"),
                "Player transfer"
        );

        if (!result.successful()) {
            messages().send(sender, "<error>Payment failed<secondary>: <primary>%message%", Map.of("message", result.message()));
            return true;
        }

        messages().send(
                sender,
                "<success>Sent <highlight>%amount%<success> to <highlight>%player%<success>.",
                Map.of("amount", economy().format(result.currencyId(), result.amount()), "player", displayName(target))
        );
        return true;
    }

    private boolean handlePayToggle(final CommandSender sender, final String[] args, final int optionIndex) {
        if (!(sender instanceof Player player)) {
            messages().send(sender, "<error>Only players can use /paytoggle.");
            return true;
        }
        if (!sender.hasPermission("coffers.command.paytoggle")) {
            messages().send(sender, "<error>You do not have permission to manage Coffers payment preferences.");
            return true;
        }

        final Boolean desiredState;
        if (args.length <= optionIndex) {
            desiredState = null;
        } else {
            final String option = args[optionIndex].toLowerCase(Locale.ROOT);
            if ("on".equals(option) || "enable".equals(option) || "enabled".equals(option)) {
                desiredState = Boolean.TRUE;
            } else if ("off".equals(option) || "disable".equals(option) || "disabled".equals(option)) {
                desiredState = Boolean.FALSE;
            } else {
                messages().send(sender, "<usage>Usage<secondary>: <primary>/paytoggle [on|off]");
                return true;
            }
        }

        final boolean paymentsEnabled = desiredState == null
                ? this.plugin.paymentPreferences().togglePayments(player.getUniqueId())
                : this.plugin.paymentPreferences().setAllowsPayments(player.getUniqueId(), desiredState.booleanValue());

        if (paymentsEnabled) {
            messages().send(sender, "<success>You are now accepting Coffers payments.");
        } else {
            messages().send(sender, "<warning>You are no longer accepting Coffers payments.");
        }
        return true;
    }

    private boolean handleSet(final CommandSender sender, final String[] args) {
        if (!sender.hasPermission("coffers.command.set")) {
            messages().send(sender, "<error>You do not have permission to set balances.");
            return true;
        }

        if (args.length < 3) {
            messages().send(sender, "<usage>Usage<secondary>: <primary>/coffers set <player> <amount> [currency]");
            return true;
        }

        final OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        final BigDecimal amount = parseAmount(sender, args[2]);
        if (amount == null) {
            return true;
        }
        final String currencyId = args.length >= 4 ? args[3] : economy().defaultCurrencyId();

        final TransactionResult result = economy().setBalance(
                target.getUniqueId(),
                currencyId,
                amount,
                actorFromSender(sender, "command:/coffers set"),
                "Admin set balance"
        );
        if (!result.successful()) {
            messages().send(sender, "<error>Balance update failed<secondary>: <primary>%message%", Map.of("message", result.message()));
            return true;
        }
        messages().send(
                sender,
                "<success>Set <highlight>%player%<success> to <highlight>%amount%<success>.",
                Map.of("player", displayName(target), "amount", economy().format(result.currencyId(), result.balance()))
        );
        return true;
    }

    private boolean handleHistory(final CommandSender sender, final String[] args) {
        if (!sender.hasPermission("coffers.command.history")) {
            messages().send(sender, "<error>You do not have permission to view Coffers history.");
            return true;
        }

        final OfflinePlayer target;
        final int optionStartIndex;
        if (args.length == 1 || looksLikeHistoryOption(args[1])) {
            if (!(sender instanceof Player player)) {
                messages().send(sender, "<error>Console must specify a player.");
                return true;
            }
            target = player;
            optionStartIndex = 1;
        } else {
            target = Bukkit.getOfflinePlayer(args[1]);
            if (!isSelfTarget(sender, target) && !sender.hasPermission("coffers.command.history.others")) {
                messages().send(sender, "<error>You do not have permission to view another player's Coffers history.");
                return true;
            }
            optionStartIndex = 2;
        }

        final HistoryOptions options = parseHistoryOptions(args, optionStartIndex);
        final List<LedgerEntry> entries = this.plugin.economyService().filteredTransactionHistory(
                target.getUniqueId(),
                options.offset(),
                options.limit(),
                options.currencyId(),
                options.kind()
        );
        if (entries.isEmpty()) {
            messages().send(sender, "<warning>No Coffers history exists for <highlight>%player%<warning>.", Map.of("player", displayName(target)));
            return true;
        }

        messages().send(
                sender,
                "<accent>Coffers history for <highlight>%player%<accent> (page <highlight>%page%<accent>, limit <highlight>%limit%<accent>):",
                Map.of("player", displayName(target), "page", Integer.toString(options.page()), "limit", Integer.toString(options.limit()))
        );
        for (final LedgerEntry entry : entries) {
            messages().send(sender, "<bullet><primary>%entry%", Map.of("entry", describeEntry(entry)));
        }
        return true;
    }

    private boolean handleCurrencies(final CommandSender sender) {
        if (!sender.hasPermission("coffers.command.currencies")) {
            messages().send(sender, "<error>You do not have permission to view Coffers currencies.");
            return true;
        }
        final Collection<String> descriptions = economy().currencies().stream()
                .map(currency -> currency.id() + " -> " + economy().format(currency.id(), currency.startingBalance()))
                .toList();
        messages().send(sender, "<accent>Available Coffers currencies:");
        for (final String description : descriptions) {
            messages().send(sender, "<bullet><primary>%currency%", Map.of("currency", description));
        }
        return true;
    }

    private boolean handleMigrateVault(final CommandSender sender, final String[] args) {
        if (!sender.hasPermission("coffers.command.migratevault")) {
            messages().send(sender, "<error>You do not have permission to migrate balances from another Vault economy provider.");
            return true;
        }

        if (!migrationService().available()) {
            messages().send(sender, "<warning>Vault migration is unavailable because Vault is not installed.");
            return true;
        }

        final String providerName = args.length >= 2 ? args[1] : null;
        try {
            final MigrationReport report = migrationService().migrate(providerName);
            this.plugin.diagnostics().writeMigrationReport(providerName, report, null, migrationService().availableProviders());
            messages().send(sender, "<success>Migration completed from <highlight>%provider%<success>.", Map.of("provider", report.providerName()));
            messages().send(sender, "<bullet><primary>Imported accounts<secondary>: <highlight>%count%", Map.of("count", Integer.toString(report.importedAccounts())));
            messages().send(sender, "<bullet><primary>Updated accounts<secondary>: <highlight>%count%", Map.of("count", Integer.toString(report.updatedAccounts())));
            messages().send(sender, "<bullet><primary>Skipped accounts<secondary>: <highlight>%count%", Map.of("count", Integer.toString(report.skippedAccounts())));
        } catch (final IllegalStateException exception) {
            this.plugin.diagnostics().writeMigrationReport(providerName, null, exception, migrationService().availableProviders());
            messages().send(sender, "<error>Migration failed<secondary>: <primary>%message%", Map.of("message", exception.getMessage()));
            final List<String> providers = migrationService().availableProviders();
            if (!providers.isEmpty()) {
                messages().send(sender, "<bullet><primary>Available providers<secondary>: <highlight>%providers%", Map.of("providers", String.join(", ", providers)));
            }
        }
        return true;
    }

    private boolean handleReload(final CommandSender sender) {
        if (!sender.hasPermission("coffers.command.reload")) {
            messages().send(sender, "<error>You do not have permission to reload Coffers.");
            return true;
        }

        if (this.plugin.reloadRuntime()) {
            messages().send(sender, "<success>Reloaded Coffers successfully.");
        } else {
            messages().send(sender, "<error>Coffers reload failed. Check the console for config or storage errors.");
        }
        return true;
    }

    private boolean handleStatus(final CommandSender sender) {
        if (!sender.hasPermission("coffers.command.status")) {
            messages().send(sender, "<error>You do not have permission to view Coffers status.");
            return true;
        }

        final String vaultStatus;
        if (this.plugin.isVaultCompatibilityActive()) {
            vaultStatus = "active";
        } else if (!this.plugin.isVaultInstalled()) {
            vaultStatus = "standalone";
        } else if ("disabled".equalsIgnoreCase(this.plugin.configuredVaultBridgeMode())) {
            vaultStatus = "disabled by config";
        } else {
            vaultStatus = "available but inactive";
        }

        messages().send(sender, "<accent>Coffers status overview:");
        messages().send(sender, "<bullet><primary>Jar line<secondary>: <highlight>modern");
        messages().send(sender, "<bullet><primary>Storage backend<secondary>: <highlight>%storage%", Map.of("storage", this.plugin.configuredStorageType()));
        messages().send(sender, "<bullet><primary>Default currency<secondary>: <highlight>%currency%", Map.of("currency", economy().defaultCurrencyId()));
        messages().send(sender, "<bullet><primary>Configured currencies<secondary>: <highlight>%count%", Map.of("count", Integer.toString(economy().currencies().size())));
        messages().send(sender, "<bullet><primary>Vault bridge mode<secondary>: <highlight>%mode%", Map.of("mode", this.plugin.configuredVaultBridgeMode()));
        messages().send(sender, "<bullet><primary>Vault runtime state<secondary>: <highlight>%state%", Map.of("state", vaultStatus));
        messages().send(sender, "<bullet><primary>PlaceholderAPI<secondary>: <highlight>%state%", Map.of("state", this.plugin.isPlaceholderExpansionActive() ? "active" : "not active"));
        messages().send(sender, "<bullet><primary>Payments blocked<secondary>: <highlight>%count%", Map.of("count", Integer.toString(this.plugin.paymentPreferences().disabledPaymentCount())));
        messages().send(sender, "<bullet><primary>Registered banks<secondary>: <highlight>%count%", Map.of("count", Integer.toString(this.plugin.bankRegistry().bankCount())));
        messages().send(sender, "<bullet><primary>Automatic backups<secondary>: <highlight>%state%", Map.of("state", this.plugin.autoBackupsEnabled() ? "enabled" : "disabled"));
        messages().send(sender, "<bullet><primary>Backup interval<secondary>: <highlight>%minutes% minute(s)", Map.of("minutes", Integer.toString(this.plugin.autoBackupIntervalMinutes())));
        messages().send(sender, "<bullet><primary>Automated retention<secondary>: <highlight>%count%", Map.of("count", Integer.toString(this.plugin.automatedBackupRetentionCount())));
        messages().send(sender, "<bullet><primary>Latest report folder<secondary>: <highlight>plugins/%plugin%/reports", Map.of("plugin", this.plugin.getDataFolder().getName()));
        return true;
    }

    private boolean handleTop(final CommandSender sender, final String[] args, final int optionOffset) {
        if (!sender.hasPermission("coffers.command.top")) {
            messages().send(sender, "<error>You do not have permission to view Coffers leaderboards.");
            return true;
        }
        String currencyId = economy().defaultCurrencyId();
        int limit = 10;

        if (args.length > optionOffset) {
            if (isInteger(args[optionOffset])) {
                limit = parseLimit(args[optionOffset]);
            } else {
                currencyId = args[optionOffset];
            }
        }
        if (args.length > optionOffset + 1) {
            limit = parseLimit(args[optionOffset + 1]);
        }

        final List<AccountSnapshot> topAccounts = this.plugin.economyService().topAccounts(
                currencyId,
                Math.min(limit, 25),
                accountId -> !this.plugin.bankRegistry().isBankAccount(accountId)
        );
        messages().send(sender, "<accent>Top Coffers accounts for <highlight>%currency%<accent>:", Map.of("currency", currencyId));
        if (topAccounts.isEmpty()) {
            messages().send(sender, "<warning>No account data is available yet.");
            return true;
        }

        for (int index = 0; index < topAccounts.size(); index++) {
            final AccountSnapshot snapshot = topAccounts.get(index);
            final OfflinePlayer player = Bukkit.getOfflinePlayer(snapshot.accountId());
            messages().send(
                    sender,
                    "<bullet><accent>%rank%. <highlight>%player%<secondary> - <primary>%amount%",
                    Map.of(
                            "rank", Integer.toString(index + 1),
                            "player", displayName(player),
                            "amount", economy().format(currencyId, snapshot.balance())
                    )
            );
        }
        return true;
    }

    private boolean handleRollback(final CommandSender sender, final String[] args) {
        if (!sender.hasPermission("coffers.command.rollback")) {
            messages().send(sender, "<error>You do not have permission to roll back Coffers transactions.");
            return true;
        }
        if (args.length < 2) {
            messages().send(sender, "<usage>Usage<secondary>: <primary>/coffers rollback <entry-id>");
            return true;
        }

        final UUID entryId;
        try {
            entryId = UUID.fromString(args[1]);
        } catch (final IllegalArgumentException exception) {
            messages().send(sender, "<error>That transaction id is not a valid UUID.");
            return true;
        }

        final TransactionResult result = this.plugin.economyService().rollback(
                entryId,
                actorFromSender(sender, "command:/coffers rollback"),
                "Rollback requested by " + sender.getName()
        );
        if (!result.successful()) {
            messages().send(sender, "<error>Rollback failed<secondary>: <primary>%message%", Map.of("message", result.message()));
            return true;
        }

        messages().send(
                sender,
                "<success>Rolled back transaction <highlight>%entry%<success>. New balance: <highlight>%balance%",
                Map.of("entry", entryId.toString(), "balance", economy().format(result.currencyId(), result.balance()))
        );
        return true;
    }

    private boolean handleBank(final CommandSender sender, final String[] args) {
        if (args.length < 3) {
            sendBankUsage(sender);
            return true;
        }

        final String action = args[1].toLowerCase(Locale.ROOT);
        final String bankName = args[2];
        return switch (action) {
            case "create" -> handleBankCreate(sender, bankName);
            case "delete", "remove" -> handleBankDelete(sender, bankName);
            case "balance" -> handleBankBalance(sender, args, bankName);
            case "deposit" -> handleBankDeposit(sender, args, bankName);
            case "withdraw" -> handleBankWithdraw(sender, args, bankName);
            case "history" -> handleBankHistory(sender, args, bankName);
            default -> {
                sendBankUsage(sender);
                yield true;
            }
        };
    }

    private boolean handleBackup(final CommandSender sender, final String[] args) {
        if (!sender.hasPermission("coffers.command.backup")) {
            messages().send(sender, "<error>You do not have permission to create Coffers backups.");
            return true;
        }

        final String backupName = args.length >= 2 ? args[1] : null;
        try {
            final File backupFile = this.plugin.archiveService().backup(
                backupName,
                this.plugin.economyService().snapshot(),
                this.plugin.paymentPreferences().disabledPaymentAccounts(),
                this.plugin.bankRegistry().snapshotBanks()
        );
            final StorageSnapshot snapshot = this.plugin.economyService().snapshot();
            this.plugin.diagnostics().writeBackupReport(
                    "manual",
                    backupFile,
                    0,
                    snapshot.balances().size(),
                    snapshot.history().size(),
                    this.plugin.paymentPreferences().disabledPaymentCount(),
                    this.plugin.bankRegistry().bankCount()
            );
            messages().send(sender, "<success>Created Coffers backup<secondary>: <highlight>%file%", Map.of("file", backupFile.getName()));
        } catch (final Exception exception) {
            messages().send(sender, "<error>Backup failed<secondary>: <primary>%message%", Map.of("message", exception.getMessage()));
        }
        return true;
    }

    private boolean handleExport(final CommandSender sender, final String[] args) {
        if (!sender.hasPermission("coffers.command.export")) {
            messages().send(sender, "<error>You do not have permission to export Coffers data.");
            return true;
        }

        final String exportName = args.length >= 2 ? args[1] : null;
        try {
            final File exportFile = this.plugin.archiveService().export(
                exportName,
                this.plugin.economyService().snapshot(),
                this.plugin.paymentPreferences().disabledPaymentAccounts(),
                this.plugin.bankRegistry().snapshotBanks()
        );
            messages().send(sender, "<success>Exported Coffers data to <highlight>%file%", Map.of("file", exportFile.getName()));
        } catch (final Exception exception) {
            messages().send(sender, "<error>Export failed<secondary>: <primary>%message%", Map.of("message", exception.getMessage()));
        }
        return true;
    }

    private boolean handleImport(final CommandSender sender, final String[] args) {
        if (!sender.hasPermission("coffers.command.import")) {
            messages().send(sender, "<error>You do not have permission to import Coffers data.");
            return true;
        }
        if (args.length < 2) {
            messages().send(sender, "<usage>Usage<secondary>: <primary>/coffers import <export-name>");
            return true;
        }

        try {
            final ArchiveSnapshot imported = this.plugin.archiveService().importSnapshot(args[1]);
            this.plugin.economyService().replaceSnapshot(imported.storageSnapshot());
            this.plugin.paymentPreferences().replaceDisabledPaymentAccounts(imported.disabledPaymentAccounts());
            this.plugin.bankRegistry().replaceBanks(imported.banks());
            messages().send(sender, "<success>Imported Coffers data from <highlight>%name%<success>.", Map.of("name", args[1]));
        } catch (final Exception exception) {
            messages().send(sender, "<error>Import failed<secondary>: <primary>%message%", Map.of("message", exception.getMessage()));
        }
        return true;
    }

    private boolean handleRestore(final CommandSender sender, final String[] args) {
        if (!sender.hasPermission("coffers.command.restore")) {
            messages().send(sender, "<error>You do not have permission to restore Coffers backups.");
            return true;
        }

        final String backupName = args.length >= 2 ? args[1] : "latest";
        try {
            final ResolvedArchiveSnapshot source = this.plugin.archiveService().resolve(backupName);
            final ArchiveSnapshot restored = source.snapshot();
            this.plugin.economyService().replaceSnapshot(restored.storageSnapshot());
            this.plugin.paymentPreferences().replaceDisabledPaymentAccounts(restored.disabledPaymentAccounts());
            this.plugin.bankRegistry().replaceBanks(restored.banks());
            messages().send(
                    sender,
                    "<success>Restored Coffers %type% <highlight>%name%<success> without restarting the server.",
                    Map.of("type", source.sourceType(), "name", source.sourceName())
            );
        } catch (final Exception exception) {
            messages().send(sender, "<error>Backup restore failed<secondary>: <primary>%message%", Map.of("message", exception.getMessage()));
        }
        return true;
    }

    private boolean handleRestorePreview(final CommandSender sender, final String[] args) {
        if (!sender.hasPermission("coffers.command.restorepreview")) {
            messages().send(sender, "<error>You do not have permission to preview Coffers restores.");
            return true;
        }
        if (args.length < 2) {
            messages().send(sender, "<usage>Usage<secondary>: <primary>/coffers restorepreview <backup-name|latest|export:name>");
            return true;
        }

        try {
            final ResolvedArchiveSnapshot source = this.plugin.archiveService().resolve(args[1]);
            final StorageSnapshot current = this.plugin.economyService().snapshot();
            final StorageSnapshot incoming = source.snapshot().storageSnapshot();

            final Set<UUID> currentAccounts = accountIds(current);
            final Set<UUID> incomingAccounts = accountIds(incoming);
            int changedAccounts = 0;
            for (final UUID accountId : incomingAccounts) {
                if (currentAccounts.contains(accountId)
                        && (!Objects.equals(current.balances().get(accountId), incoming.balances().get(accountId))
                        || !Objects.equals(current.history().get(accountId), incoming.history().get(accountId)))) {
                    changedAccounts++;
                }
            }

            final Set<String> currentBankKeys = this.plugin.bankRegistry().snapshotBanks().keySet();
            final Set<String> incomingBankKeys = source.snapshot().banks().keySet();
            int changedBanks = 0;
            for (final String bankKey : incomingBankKeys) {
                if (currentBankKeys.contains(bankKey)
                        && !Objects.equals(this.plugin.bankRegistry().snapshotBanks().get(bankKey), source.snapshot().banks().get(bankKey))) {
                    changedBanks++;
                }
            }

            final Set<UUID> currentDisabled = this.plugin.paymentPreferences().disabledPaymentAccounts();
            final Set<UUID> incomingDisabled = source.snapshot().disabledPaymentAccounts();
            final Set<UUID> disabledDifference = new LinkedHashSet<>(currentDisabled);
            disabledDifference.removeAll(incomingDisabled);
            for (final UUID accountId : incomingDisabled) {
                if (!currentDisabled.contains(accountId)) {
                    disabledDifference.add(accountId);
                }
            }

            final Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("current-account-count", currentAccounts.size());
            summary.put("incoming-account-count", incomingAccounts.size());
            summary.put("accounts-added", differenceSize(incomingAccounts, currentAccounts));
            summary.put("accounts-removed", differenceSize(currentAccounts, incomingAccounts));
            summary.put("accounts-changed", changedAccounts);
            summary.put("current-bank-count", currentBankKeys.size());
            summary.put("incoming-bank-count", incomingBankKeys.size());
            summary.put("banks-added", differenceSize(incomingBankKeys, currentBankKeys));
            summary.put("banks-removed", differenceSize(currentBankKeys, incomingBankKeys));
            summary.put("banks-changed", changedBanks);
            summary.put("disabled-payment-changes", disabledDifference.size());
            final File reportFile = this.plugin.diagnostics().writeRestorePreviewReport(
                    "full-restore",
                    source.sourceType(),
                    source.sourceName(),
                    summary,
                    List.of("Preview only. No Coffers data was changed.")
            );
            messages().send(sender, "<accent>Coffers restore preview for <highlight>%type%:%name%<accent>:", Map.of("type", source.sourceType(), "name", source.sourceName()));
            messages().send(sender, "<bullet><primary>Accounts added<secondary>: <highlight>%count%", Map.of("count", summary.get("accounts-added").toString()));
            messages().send(sender, "<bullet><primary>Accounts changed<secondary>: <highlight>%count%", Map.of("count", summary.get("accounts-changed").toString()));
            messages().send(sender, "<bullet><primary>Accounts removed<secondary>: <highlight>%count%", Map.of("count", summary.get("accounts-removed").toString()));
            messages().send(sender, "<bullet><primary>Banks changed<secondary>: <highlight>%count%", Map.of("count", summary.get("banks-changed").toString()));
            messages().send(sender, "<bullet><primary>Payment preference changes<secondary>: <highlight>%count%", Map.of("count", summary.get("disabled-payment-changes").toString()));
            messages().send(sender, "<bullet><primary>Preview report<secondary>: <highlight>%file%", Map.of("file", reportFile.getName()));
        } catch (final Exception exception) {
            messages().send(sender, "<error>Restore preview failed<secondary>: <primary>%message%", Map.of("message", exception.getMessage()));
        }
        return true;
    }

    private boolean handleRestoreAccount(final CommandSender sender, final String[] args) {
        if (!sender.hasPermission("coffers.command.restoreaccount")) {
            messages().send(sender, "<error>You do not have permission to restore individual Coffers accounts.");
            return true;
        }
        if (args.length < 3) {
            messages().send(sender, "<usage>Usage<secondary>: <primary>/coffers restoreaccount <backup-name|latest|export:name> <player|uuid|bank:name> [dryrun]");
            return true;
        }

        final boolean dryRun = args.length >= 4 && "dryrun".equalsIgnoreCase(args[3]);
        try {
            final ResolvedArchiveSnapshot source = this.plugin.archiveService().resolve(args[1]);
            final String targetSpec = args[2];
            if (targetSpec.toLowerCase(Locale.ROOT).startsWith("bank:")) {
                return handleRestoreBankAccount(sender, source, targetSpec.substring("bank:".length()), dryRun);
            }
            return handleRestorePlayerAccount(sender, source, targetSpec, dryRun);
        } catch (final Exception exception) {
            messages().send(sender, "<error>Account restore failed<secondary>: <primary>%message%", Map.of("message", exception.getMessage()));
            return true;
        }
    }

    private boolean handleValidate(final CommandSender sender) {
        if (!sender.hasPermission("coffers.command.validate")) {
            messages().send(sender, "<error>You do not have permission to validate Coffers data.");
            return true;
        }

        final StorageSnapshot snapshot = this.plugin.economyService().snapshot();
        final Set<UUID> accountIds = accountIds(snapshot);
        final Map<String, String> bankSnapshot = this.plugin.bankRegistry().snapshotBanks();
        final List<String> issues = new ArrayList<>();
        final Set<String> validCurrencies = new LinkedHashSet<>(currencyIds());
        final Set<String> seenBankDisplays = new LinkedHashSet<>();

        for (final Map.Entry<UUID, Map<String, BigDecimal>> entry : snapshot.balances().entrySet()) {
            for (final Map.Entry<String, BigDecimal> balance : entry.getValue().entrySet()) {
                if (!validCurrencies.contains(balance.getKey())) {
                    issues.add("Account " + entry.getKey() + " contains unknown currency id '" + balance.getKey() + "'.");
                }
                if (balance.getValue() != null && balance.getValue().signum() < 0) {
                    issues.add("Account " + entry.getKey() + " has a negative balance in currency '" + balance.getKey() + "'.");
                }
            }
        }

        for (final Map.Entry<UUID, List<LedgerEntry>> entry : snapshot.history().entrySet()) {
            if (!snapshot.balances().containsKey(entry.getKey())) {
                issues.add("History exists for account " + entry.getKey() + " but no live balance snapshot is present.");
            }
            for (final LedgerEntry ledgerEntry : entry.getValue()) {
                if (!validCurrencies.contains(ledgerEntry.currencyId())) {
                    issues.add("Ledger entry " + ledgerEntry.entryId() + " uses unknown currency id '" + ledgerEntry.currencyId() + "'.");
                }
                if (ledgerEntry.resultingBalance().signum() < 0) {
                    issues.add("Ledger entry " + ledgerEntry.entryId() + " results in a negative balance.");
                }
            }
        }

        for (final Map.Entry<String, String> bank : bankSnapshot.entrySet()) {
            final UUID bankAccountId = this.plugin.bankRegistry().bankAccountIdForKey(bank.getKey());
            if (!accountIds.contains(bankAccountId)) {
                issues.add("Bank '" + bank.getValue() + "' is registered but its backing account snapshot is missing.");
            }
            final String normalizedDisplay = bank.getValue().toLowerCase(Locale.ROOT);
            if (!seenBankDisplays.add(normalizedDisplay)) {
                issues.add("Duplicate bank display name detected: '" + bank.getValue() + "'.");
            }
        }

        final File reportFile = this.plugin.diagnostics().writeValidationReport(
                "runtime",
                snapshot.balances().size(),
                snapshot.history().size(),
                this.plugin.paymentPreferences().disabledPaymentCount(),
                bankSnapshot.size(),
                issues
        );
        if (issues.isEmpty()) {
            messages().send(sender, "<success>Coffers validation completed without issues.");
        } else {
            messages().send(sender, "<warning>Coffers validation found <highlight>%count%<warning> issue(s).", Map.of("count", Integer.toString(issues.size())));
        }
        messages().send(sender, "<bullet><primary>Validation report<secondary>: <highlight>%file%", Map.of("file", reportFile.getName()));
        return true;
    }

    private BigDecimal parseAmount(final CommandSender sender, final String raw) {
        try {
            final BigDecimal amount = new BigDecimal(raw);
            if (amount.signum() < 0) {
                messages().send(sender, "<error>Amount must not be negative.");
                return null;
            }
            return amount;
        } catch (final NumberFormatException exception) {
            messages().send(sender, "<error>Invalid amount<secondary>: <primary>%amount%", Map.of("amount", raw));
            return null;
        }
    }

    private boolean handleBankCreate(final CommandSender sender, final String bankName) {
        if (!sender.hasPermission("coffers.command.bank.create")) {
            messages().send(sender, "<error>You do not have permission to create Coffers banks.");
            return true;
        }
        if (!this.plugin.bankRegistry().createBank(bankName)) {
            messages().send(sender, "<warning>That Coffers bank already exists or the name is invalid.");
            return true;
        }
        economy().createAccount(this.plugin.bankRegistry().bankAccountId(bankName));
        messages().send(sender, "<success>Created Coffers bank <highlight>%bank%<success>.", Map.of("bank", bankName));
        return true;
    }

    private boolean handleBankDelete(final CommandSender sender, final String bankName) {
        if (!sender.hasPermission("coffers.command.bank.delete")) {
            messages().send(sender, "<error>You do not have permission to delete Coffers banks.");
            return true;
        }
        if (!this.plugin.bankRegistry().exists(bankName)) {
            messages().send(sender, "<warning>No Coffers bank exists with that name.");
            return true;
        }
        final UUID bankId = this.plugin.bankRegistry().bankAccountId(bankName);
        this.plugin.economyService().purgeAccount(bankId);
        this.plugin.bankRegistry().deleteBank(bankName);
        messages().send(sender, "<success>Deleted Coffers bank <highlight>%bank%<success>.", Map.of("bank", bankName));
        return true;
    }

    private boolean handleBankBalance(final CommandSender sender, final String[] args, final String bankName) {
        if (!sender.hasPermission("coffers.command.bank.balance")) {
            messages().send(sender, "<error>You do not have permission to view Coffers bank balances.");
            return true;
        }
        final UUID bankId = bankId(bankName, sender);
        if (bankId == null) {
            return true;
        }
        final String currencyId = args.length >= 4 ? args[3] : economy().defaultCurrencyId();
        final BigDecimal balance = economy().getBalance(bankId, currencyId);
        messages().send(sender, "<success>Bank balance for <highlight>%bank%<success>: <highlight>%balance%", Map.of("bank", bankName, "balance", economy().format(currencyId, balance)));
        return true;
    }

    private boolean handleBankDeposit(final CommandSender sender, final String[] args, final String bankName) {
        if (!sender.hasPermission("coffers.command.bank.deposit")) {
            messages().send(sender, "<error>You do not have permission to deposit into Coffers banks.");
            return true;
        }
        if (args.length < 4) {
            messages().send(sender, "<usage>Usage<secondary>: <primary>/coffers bank deposit <name> <amount> [currency]");
            return true;
        }
        final UUID bankId = bankId(bankName, sender);
        if (bankId == null) {
            return true;
        }
        final BigDecimal amount = parseAmount(sender, args[3]);
        if (amount == null) {
            return true;
        }
        final String currencyId = args.length >= 5 ? args[4] : economy().defaultCurrencyId();
        final TransactionResult result = economy().deposit(bankId, currencyId, amount, actorFromSender(sender, "command:/coffers bank deposit"), "Bank deposit");
        if (!result.successful()) {
            messages().send(sender, "<error>Bank deposit failed<secondary>: <primary>%message%", Map.of("message", result.message()));
            return true;
        }
        messages().send(sender, "<success>Deposited <highlight>%amount%<success> into bank <highlight>%bank%<success>.", Map.of("amount", economy().format(currencyId, amount), "bank", bankName));
        return true;
    }

    private boolean handleBankWithdraw(final CommandSender sender, final String[] args, final String bankName) {
        if (!sender.hasPermission("coffers.command.bank.withdraw")) {
            messages().send(sender, "<error>You do not have permission to withdraw from Coffers banks.");
            return true;
        }
        if (args.length < 4) {
            messages().send(sender, "<usage>Usage<secondary>: <primary>/coffers bank withdraw <name> <amount> [currency]");
            return true;
        }
        final UUID bankId = bankId(bankName, sender);
        if (bankId == null) {
            return true;
        }
        final BigDecimal amount = parseAmount(sender, args[3]);
        if (amount == null) {
            return true;
        }
        final String currencyId = args.length >= 5 ? args[4] : economy().defaultCurrencyId();
        final TransactionResult result = economy().withdraw(bankId, currencyId, amount, actorFromSender(sender, "command:/coffers bank withdraw"), "Bank withdrawal");
        if (!result.successful()) {
            messages().send(sender, "<error>Bank withdrawal failed<secondary>: <primary>%message%", Map.of("message", result.message()));
            return true;
        }
        messages().send(sender, "<success>Withdrew <highlight>%amount%<success> from bank <highlight>%bank%<success>.", Map.of("amount", economy().format(currencyId, amount), "bank", bankName));
        return true;
    }

    private boolean handleBankHistory(final CommandSender sender, final String[] args, final String bankName) {
        if (!sender.hasPermission("coffers.command.bank.history")) {
            messages().send(sender, "<error>You do not have permission to view Coffers bank history.");
            return true;
        }
        final UUID bankId = bankId(bankName, sender);
        if (bankId == null) {
            return true;
        }
        final HistoryOptions options = parseHistoryOptions(args, 3);
        final List<LedgerEntry> entries = this.plugin.economyService().filteredTransactionHistory(bankId, options.offset(), options.limit(), options.currencyId(), options.kind());
        if (entries.isEmpty()) {
            messages().send(sender, "<warning>No Coffers bank history exists for <highlight>%bank%<warning>.", Map.of("bank", bankName));
            return true;
        }
        messages().send(sender, "<accent>Coffers bank history for <highlight>%bank%<accent>:", Map.of("bank", bankName));
        for (final LedgerEntry entry : entries) {
            messages().send(sender, "<bullet><primary>%entry%", Map.of("entry", describeEntry(entry)));
        }
        return true;
    }

    private UUID bankId(final String bankName, final CommandSender sender) {
        if (!this.plugin.bankRegistry().exists(bankName)) {
            messages().send(sender, "<warning>No Coffers bank exists with that name.");
            return null;
        }
        return this.plugin.bankRegistry().bankAccountId(bankName);
    }

    private void sendBankUsage(final CommandSender sender) {
        messages().send(sender, "<accent>Coffers bank commands:");
        messages().send(sender, "<bullet><usage>/coffers bank create <name>");
        messages().send(sender, "<bullet><usage>/coffers bank delete <name>");
        messages().send(sender, "<bullet><usage>/coffers bank balance <name> [currency]");
        messages().send(sender, "<bullet><usage>/coffers bank deposit <name> <amount> [currency]");
        messages().send(sender, "<bullet><usage>/coffers bank withdraw <name> <amount> [currency]");
        messages().send(sender, "<bullet><usage>/coffers bank history <name> [page=<n>] [limit=<n>] [currency=<id>] [kind=<kind>]");
    }

    private HistoryOptions parseHistoryOptions(final String[] args, final int startIndex) {
        int page = 1;
        int limit = 5;
        String currencyId = null;
        TransactionKind kind = null;

        for (int index = startIndex; index < args.length; index++) {
            final String token = args[index];
            if (token.startsWith("page=")) {
                page = Math.max(1, parseLimit(token.substring("page=".length())));
            } else if (token.startsWith("limit=")) {
                limit = Math.max(1, parseLimit(token.substring("limit=".length())));
            } else if (token.startsWith("currency=")) {
                currencyId = token.substring("currency=".length());
            } else if (token.startsWith("kind=") || token.startsWith("type=")) {
                final String rawKind = token.substring(token.indexOf('=') + 1).toUpperCase(Locale.ROOT);
                kind = TransactionKind.valueOf(rawKind);
            } else if (isInteger(token)) {
                limit = Math.max(1, parseLimit(token));
            } else if (currencyId == null && economy().currency(token).isPresent()) {
                currencyId = token;
            }
        }
        return new HistoryOptions(page, limit, currencyId, kind);
    }

    private boolean looksLikeHistoryOption(final String token) {
        return token.contains("=") || isInteger(token) || token.equalsIgnoreCase("deposit")
                || token.equalsIgnoreCase("withdrawal") || token.equalsIgnoreCase("transfer_in")
                || token.equalsIgnoreCase("transfer_out") || token.equalsIgnoreCase("set");
    }

    private String displayName(final OfflinePlayer player) {
        final String bankName = this.plugin.bankRegistry().displayName(player.getUniqueId());
        if (bankName != null) {
            return "[Bank] " + bankName;
        }
        return player.getName() != null ? player.getName() : player.getUniqueId().toString();
    }

    private void sendUsage(final CommandSender sender) {
        messages().send(sender, "<accent>Coffers commands:");
        messages().send(sender, "<bullet><usage>/coffers balance [player] [currency]");
        messages().send(sender, "<bullet><usage>/coffers pay <player> <amount> [currency]");
        messages().send(sender, "<bullet><usage>/paytoggle [on|off]");
        messages().send(sender, "<bullet><usage>/coffers set <player> <amount> [currency]");
        messages().send(sender, "<bullet><usage>/coffers history [player] [limit]");
        messages().send(sender, "<bullet><usage>/coffers rollback <entry-id>");
        messages().send(sender, "<bullet><usage>/coffers bank <create|delete|balance|deposit|withdraw|history> ...");
        messages().send(sender, "<bullet><usage>/coffers status");
        messages().send(sender, "<bullet><usage>/baltop [currency] [limit]");
        messages().send(sender, "<bullet><usage>/coffers currencies");
        messages().send(sender, "<bullet><usage>/coffers migratevault [provider]");
        messages().send(sender, "<bullet><usage>/coffers reload");
        messages().send(sender, "<bullet><usage>/coffers backup [name]");
        messages().send(sender, "<bullet><usage>/coffers export [name]");
        messages().send(sender, "<bullet><usage>/coffers import <name>");
        messages().send(sender, "<bullet><usage>/coffers restore [backup-name|latest]");
        messages().send(sender, "<bullet><usage>/coffers restorepreview <backup-name|latest|export:name>");
        messages().send(sender, "<bullet><usage>/coffers restoreaccount <source> <player|uuid|bank:name> [dryrun]");
        messages().send(sender, "<bullet><usage>/coffers validate");
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
        if (this.plugin.economy() == null) {
            return List.of();
        }

        final String commandName = command.getName().toLowerCase(Locale.ROOT);
        if ("baltop".equals(commandName)) {
            if (args.length == 1) {
                return filter(currencyIds(), args[0]);
            }
            return List.of();
        }
        if ("paytoggle".equals(commandName)) {
            if (args.length == 1) {
                return filter(List.of("on", "off"), args[0]);
            }
            return List.of();
        }

        final List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("balance");
            completions.add("pay");
            completions.add("paytoggle");
            completions.add("history");
            completions.add("rollback");
            completions.add("bank");
            completions.add("status");
            completions.add("top");
            completions.add("currencies");
            if (sender.hasPermission("coffers.command.set")) {
                completions.add("set");
            }
            if (sender.hasPermission("coffers.command.migratevault")) {
                completions.add("migratevault");
            }
            if (sender.hasPermission("coffers.command.reload")) {
                completions.add("reload");
            }
            if (sender.hasPermission("coffers.command.backup")) {
                completions.add("backup");
            }
            if (sender.hasPermission("coffers.command.export")) {
                completions.add("export");
            }
            if (sender.hasPermission("coffers.command.import")) {
                completions.add("import");
            }
            if (sender.hasPermission("coffers.command.restore")) {
                completions.add("restore");
                completions.add("restorebackup");
            }
            if (sender.hasPermission("coffers.command.restorepreview")) {
                completions.add("restorepreview");
                completions.add("previewrestore");
            }
            if (sender.hasPermission("coffers.command.restoreaccount")) {
                completions.add("restoreaccount");
            }
            if (sender.hasPermission("coffers.command.validate")) {
                completions.add("validate");
                completions.add("audit");
            }
            return filter(completions, args[0]);
        }

        if (args.length == 2 && List.of("balance", "pay", "set", "history").contains(args[0].toLowerCase(Locale.ROOT))) {
            for (final Player online : this.plugin.getServer().getOnlinePlayers()) {
                completions.add(online.getName());
            }
            return filter(completions, args[1]);
        }

        if (args.length == 2 && List.of("top", "baltop").contains(args[0].toLowerCase(Locale.ROOT))) {
            return filter(currencyIds(), args[1]);
        }

        if ((args.length == 3 && "balance".equalsIgnoreCase(args[0]))
                || (args.length == 4 && List.of("pay", "set").contains(args[0].toLowerCase(Locale.ROOT)))) {
            return filter(currencyIds(), args[args.length - 1]);
        }

        if (args.length == 2 && "paytoggle".equalsIgnoreCase(args[0])) {
            return filter(List.of("on", "off"), args[1]);
        }

        if (args.length == 2 && "migratevault".equalsIgnoreCase(args[0])) {
            if (!migrationService().available()) {
                return List.of();
            }
            return filter(migrationService().availableProviders(), args[1]);
        }

        if (args.length == 2 && "import".equalsIgnoreCase(args[0])) {
            return filter(this.plugin.archiveService().exportNames(), args[1]);
        }

        if (args.length == 2 && "backup".equalsIgnoreCase(args[0])) {
            return List.of();
        }

        if (args.length == 2 && ("restore".equalsIgnoreCase(args[0]) || "restorebackup".equalsIgnoreCase(args[0]) || "restorepreview".equalsIgnoreCase(args[0]) || "previewrestore".equalsIgnoreCase(args[0]) || "restoreaccount".equalsIgnoreCase(args[0]))) {
            final List<String> suggestions = new ArrayList<>();
            suggestions.add("latest");
            suggestions.addAll(this.plugin.archiveService().backupNames());
            for (final String exportName : this.plugin.archiveService().exportNames()) {
                suggestions.add("export:" + exportName);
            }
            return filter(suggestions, args[1]);
        }

        if (args.length == 3 && "restoreaccount".equalsIgnoreCase(args[0])) {
            final List<String> suggestions = new ArrayList<>();
            for (final Player online : this.plugin.getServer().getOnlinePlayers()) {
                suggestions.add(online.getName());
            }
            for (final String bankName : this.plugin.bankRegistry().banks()) {
                suggestions.add("bank:" + bankName);
            }
            return filter(suggestions, args[2]);
        }

        if (args.length == 4 && "restoreaccount".equalsIgnoreCase(args[0])) {
            return filter(List.of("dryrun"), args[3]);
        }

        if (args.length == 2 && "bank".equalsIgnoreCase(args[0])) {
            return filter(List.of("create", "delete", "balance", "deposit", "withdraw", "history"), args[1]);
        }

        if (args.length == 3 && "bank".equalsIgnoreCase(args[0]) && !"create".equalsIgnoreCase(args[1])) {
            return filter(new ArrayList<>(this.plugin.bankRegistry().banks()), args[2]);
        }

        return List.of();
    }

    private List<String> currencyIds() {
        return economy().currencies().stream().map(currency -> currency.id()).toList();
    }

    private List<String> filter(final List<String> candidates, final String token) {
        final String needle = token.toLowerCase(Locale.ROOT);
        return candidates.stream()
                .filter(candidate -> candidate.toLowerCase(Locale.ROOT).startsWith(needle))
                .toList();
    }

    private int parseLimit(final String raw) {
        try {
            return Math.max(1, Integer.parseInt(raw));
        } catch (final NumberFormatException exception) {
            return 5;
        }
    }

    private TransactionActor actorFromSender(final CommandSender sender, final String source) {
        if (sender instanceof Player player) {
            return TransactionActor.player(player.getUniqueId(), player.getName(), source);
        }
        return TransactionActor.console(source);
    }

    private String describeEntry(final LedgerEntry entry) {
        final String action;
        switch (entry.kind()) {
            case DEPOSIT -> action = "deposit";
            case WITHDRAWAL -> action = "withdrawal";
            case TRANSFER_IN -> action = "transfer in";
            case TRANSFER_OUT -> action = "transfer out";
            case SET -> action = "set balance";
            default -> throw new IllegalStateException("Unhandled transaction kind: " + entry.kind());
        }
        final StringBuilder builder = new StringBuilder();
        builder.append(action)
                .append(" ")
                .append(economy().format(entry.currencyId(), entry.amount()))
                .append(" | previous ")
                .append(economy().format(entry.currencyId(), entry.previousBalance()))
                .append(" | balance ")
                .append(economy().format(entry.currencyId(), entry.resultingBalance()));

        if (entry.counterpartyAccountId() != null && (entry.kind() == TransactionKind.TRANSFER_IN || entry.kind() == TransactionKind.TRANSFER_OUT)) {
            builder.append(" | other account ").append(accountLabel(entry.counterpartyAccountId()));
        }
        if (entry.reason() != null && !entry.reason().isBlank()) {
            builder.append(" | ").append(entry.reason());
        }
        if (entry.actor() != null && entry.actor().actorName() != null) {
            builder.append(" | by ").append(entry.actor().actorName());
        }
        if (entry.reversalOfReferenceId() != null) {
            builder.append(" | rollback of ").append(entry.reversalOfReferenceId());
        }
        return builder.toString();
    }

    private String accountLabel(final UUID accountId) {
        final String bankName = this.plugin.bankRegistry().displayName(accountId);
        if (bankName != null) {
            return "[Bank] " + bankName;
        }
        return displayName(Bukkit.getOfflinePlayer(accountId));
    }

    private CoffersEconomy economy() {
        return this.plugin.economy();
    }

    private MigrationGateway migrationService() {
        return Objects.requireNonNull(this.plugin.migrationService(), "Coffers migration service is not available.");
    }

    private CoffersMessagePalette messages() {
        return this.plugin.messages();
    }

    private boolean isInteger(final String raw) {
        try {
            Integer.parseInt(raw);
            return true;
        } catch (final NumberFormatException exception) {
            return false;
        }
    }

    private boolean isSelfTarget(final CommandSender sender, final OfflinePlayer target) {
        return sender instanceof Player player && player.getUniqueId().equals(target.getUniqueId());
    }

    private boolean handleRestorePlayerAccount(
            final CommandSender sender,
            final ResolvedArchiveSnapshot source,
            final String playerToken,
            final boolean dryRun
    ) {
        final OfflinePlayer player = resolveOfflinePlayer(playerToken);
        final UUID accountId = player.getUniqueId();
        final StorageSnapshot sourceSnapshot = source.snapshot().storageSnapshot();
        if (!containsAccount(sourceSnapshot, accountId)) {
            messages().send(sender, "<warning>No Coffers account data for <highlight>%player%<warning> exists in that %type%.", Map.of("player", displayName(player), "type", source.sourceType()));
            return true;
        }

        final StorageSnapshot currentSnapshot = this.plugin.economyService().snapshot();
        final boolean targetBlocksPaymentsNow = !this.plugin.paymentPreferences().allowsPayments(accountId);
        final boolean targetBlocksPaymentsInSource = source.snapshot().disabledPaymentAccounts().contains(accountId);
        final int balanceCurrencies = sourceSnapshot.balances().getOrDefault(accountId, Map.of()).size();
        final int historyEntries = sourceSnapshot.history().getOrDefault(accountId, List.of()).size();
        final Map<String, Object> summary = Map.of(
                "balance-currencies", balanceCurrencies,
                "history-entries", historyEntries,
                "currently-exists", containsAccount(currentSnapshot, accountId),
                "payment-preference-changed", targetBlocksPaymentsNow != targetBlocksPaymentsInSource
        );
        final File reportFile = this.plugin.diagnostics().writeAccountRestoreReport(
                displayName(player),
                source.sourceType(),
                source.sourceName(),
                dryRun,
                summary,
                List.of(dryRun ? "Preview only. No Coffers data was changed." : "Applied a targeted Coffers account restore.")
        );

        if (!dryRun) {
            this.plugin.economyService().restoreAccountFromSnapshot(accountId, sourceSnapshot);
            this.plugin.paymentPreferences().setAllowsPayments(accountId, !targetBlocksPaymentsInSource);
        }

        messages().send(sender, dryRun
                ? "<accent>Coffers restore preview prepared for <highlight>%player%<accent>."
                : "<success>Restored Coffers account data for <highlight>%player%<success>."
                , Map.of("player", displayName(player)));
        messages().send(sender, "<bullet><primary>Balance entries<secondary>: <highlight>%count%", Map.of("count", Integer.toString(balanceCurrencies)));
        messages().send(sender, "<bullet><primary>History entries<secondary>: <highlight>%count%", Map.of("count", Integer.toString(historyEntries)));
        messages().send(sender, "<bullet><primary>Report<secondary>: <highlight>%file%", Map.of("file", reportFile.getName()));
        return true;
    }

    private boolean handleRestoreBankAccount(
            final CommandSender sender,
            final ResolvedArchiveSnapshot source,
            final String bankToken,
            final boolean dryRun
    ) {
        final Map.Entry<String, String> sourceBank = findSourceBank(source.snapshot().banks(), bankToken);
        if (sourceBank == null) {
            messages().send(sender, "<warning>No Coffers bank named <highlight>%bank%<warning> exists in that %type%.", Map.of("bank", bankToken, "type", source.sourceType()));
            return true;
        }

        final String currentKey = this.plugin.bankRegistry().bankKey(sourceBank.getValue());
        if (currentKey != null && !currentKey.equals(sourceBank.getKey())) {
            messages().send(sender, "<error>A different live bank already uses the name <highlight>%bank%<error>. Rename or remove it before restoring this archived bank.", Map.of("bank", sourceBank.getValue()));
            return true;
        }

        final UUID bankAccountId = this.plugin.bankRegistry().bankAccountIdForKey(sourceBank.getKey());
        final StorageSnapshot sourceSnapshot = source.snapshot().storageSnapshot();
        if (!containsAccount(sourceSnapshot, bankAccountId)) {
            messages().send(sender, "<warning>The archived bank <highlight>%bank%<warning> does not contain account data to restore.", Map.of("bank", sourceBank.getValue()));
            return true;
        }

        final int balanceCurrencies = sourceSnapshot.balances().getOrDefault(bankAccountId, Map.of()).size();
        final int historyEntries = sourceSnapshot.history().getOrDefault(bankAccountId, List.of()).size();
        final Map<String, Object> summary = Map.of(
                "bank-key", sourceBank.getKey(),
                "balance-currencies", balanceCurrencies,
                "history-entries", historyEntries,
                "currently-registered", currentKey != null
        );
        final File reportFile = this.plugin.diagnostics().writeAccountRestoreReport(
                "bank:" + sourceBank.getValue(),
                source.sourceType(),
                source.sourceName(),
                dryRun,
                summary,
                List.of(dryRun ? "Preview only. No Coffers data was changed." : "Applied a targeted Coffers bank restore.")
        );

        if (!dryRun) {
            this.plugin.bankRegistry().ensureBank(sourceBank.getKey(), sourceBank.getValue());
            this.plugin.economyService().restoreAccountFromSnapshot(bankAccountId, sourceSnapshot);
        }

        messages().send(sender, dryRun
                ? "<accent>Coffers bank restore preview prepared for <highlight>%bank%<accent>."
                : "<success>Restored Coffers bank data for <highlight>%bank%<success>."
                , Map.of("bank", sourceBank.getValue()));
        messages().send(sender, "<bullet><primary>Balance entries<secondary>: <highlight>%count%", Map.of("count", Integer.toString(balanceCurrencies)));
        messages().send(sender, "<bullet><primary>History entries<secondary>: <highlight>%count%", Map.of("count", Integer.toString(historyEntries)));
        messages().send(sender, "<bullet><primary>Report<secondary>: <highlight>%file%", Map.of("file", reportFile.getName()));
        return true;
    }

    private OfflinePlayer resolveOfflinePlayer(final String token) {
        try {
            return Bukkit.getOfflinePlayer(UUID.fromString(token));
        } catch (final IllegalArgumentException ignored) {
            return Bukkit.getOfflinePlayer(token);
        }
    }

    private boolean containsAccount(final StorageSnapshot snapshot, final UUID accountId) {
        return snapshot.balances().containsKey(accountId) || snapshot.history().containsKey(accountId);
    }

    private Set<UUID> accountIds(final StorageSnapshot snapshot) {
        final Set<UUID> accountIds = new LinkedHashSet<>();
        accountIds.addAll(snapshot.balances().keySet());
        accountIds.addAll(snapshot.history().keySet());
        return accountIds;
    }

    private int differenceSize(final Set<?> left, final Set<?> right) {
        int count = 0;
        for (final Object entry : left) {
            if (!right.contains(entry)) {
                count++;
            }
        }
        return count;
    }

    private Map.Entry<String, String> findSourceBank(final Map<String, String> archivedBanks, final String token) {
        final String needle = token.toLowerCase(Locale.ROOT);
        for (final Map.Entry<String, String> entry : archivedBanks.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(needle) || entry.getValue().equalsIgnoreCase(token)) {
                return entry;
            }
        }
        return null;
    }

    private record HistoryOptions(int page, int limit, String currencyId, TransactionKind kind) {
        private int offset() {
            return Math.max(0, (this.page - 1) * this.limit);
        }
    }
}
