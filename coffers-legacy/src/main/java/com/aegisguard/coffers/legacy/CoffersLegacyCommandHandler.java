package com.aegisguard.coffers.legacy;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

final class CoffersLegacyCommandHandler implements CommandExecutor, TabCompleter {

    private final CoffersLegacyPlugin plugin;
    private final CoffersLegacyEconomyService economy;

    CoffersLegacyCommandHandler(final CoffersLegacyPlugin plugin, final CoffersLegacyEconomyService economy) {
        this.plugin = plugin;
        this.economy = economy;
    }

    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
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
            String action = args[0].toLowerCase(Locale.ROOT);
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
            if ("history".equals(action)) {
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
            if ("status".equals(action) || "info".equals(action)) {
                return handleStatus(sender);
            }
            if ("reload".equals(action)) {
                return handleReload(sender);
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
        } catch (IllegalArgumentException exception) {
            messages().send(sender, "<error>Coffers Legacy error<secondary>: <primary>%message%", single("message", exception.getMessage()));
            return true;
        } catch (IllegalStateException exception) {
            messages().send(sender, "<error>Coffers Legacy failed to complete that request<secondary>: <primary>%message%", single("message", exception.getMessage()));
            return true;
        }

        sendUsage(sender);
        return true;
    }

    private boolean handleBalance(final CommandSender sender, final String[] args) {
        if (args.length == 1) {
            if (!(sender instanceof Player)) {
                messages().send(sender, "<error>Console must specify a player.");
                return true;
            }

            Player player = (Player) sender;
            String currencyId = this.economy.getDefaultCurrencyId();
            BigDecimal balance = this.economy.getBalance(player.getUniqueId(), currencyId);
            messages().send(sender, "<success>Balance<secondary>: <highlight>%balance%", single("balance", this.economy.format(currencyId, balance)));
            return true;
        }

        if (args.length == 2 && sender instanceof Player && isCurrencyId(args[1])) {
            Player player = (Player) sender;
            String currencyId = args[1];
            BigDecimal balance = this.economy.getBalance(player.getUniqueId(), currencyId);
            messages().send(sender, "<success>Balance<secondary>: <highlight>%balance%", single("balance", this.economy.format(currencyId, balance)));
            return true;
        }

        if (!sender.hasPermission("cofferslegacy.command.balance.others")) {
            messages().send(sender, "<error>You do not have permission to view another player's balance.");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        String currencyId = args.length >= 3 ? args[2] : this.economy.getDefaultCurrencyId();
        BigDecimal balance = this.economy.getBalance(target.getUniqueId(), currencyId);
        messages().send(sender, "<highlight>%player%<secondary> has <highlight>%balance%", mapOf("player", displayName(target), "balance", this.economy.format(currencyId, balance)));
        return true;
    }

    private boolean handlePay(final CommandSender sender, final String[] args) {
        if (!(sender instanceof Player)) {
            messages().send(sender, "<error>Only players can use /cofferslegacy pay.");
            return true;
        }

        if (args.length < 3) {
            messages().send(sender, "<usage>Usage<secondary>: <primary>/cofferslegacy pay <player> <amount> [currency]");
            return true;
        }

        Player player = (Player) sender;
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (player.getUniqueId().equals(target.getUniqueId())) {
            messages().send(sender, "<error>You cannot pay yourself.");
            return true;
        }

        if (!this.plugin.paymentPreferences().allowsPayments(target.getUniqueId())
                && !sender.hasPermission("cofferslegacy.command.pay.ignore-toggle")) {
            messages().send(sender, "<warning>%player% is not accepting Coffers Legacy payments right now.", single("player", displayName(target)));
            return true;
        }

        BigDecimal amount = parseAmount(sender, args[2]);
        if (amount == null) {
            return true;
        }

        String currencyId = args.length >= 4 ? args[3] : this.economy.getDefaultCurrencyId();
        LegacyTransactionResult result = this.economy.transfer(
                player.getUniqueId(),
                target.getUniqueId(),
                currencyId,
                amount,
                LegacyTransactionActor.player(player.getUniqueId(), player.getName(), "command:/cofferslegacy pay"),
                "Player transfer"
        );

        if (!result.isSuccessful()) {
            messages().send(sender, "<error>Payment failed<secondary>: <primary>%message%", single("message", result.getMessage()));
            return true;
        }

        messages().send(sender, "<success>Sent <highlight>%amount%<success> to <highlight>%player%<success>.", mapOf("amount", this.economy.format(result.getCurrencyId(), result.getAmount()), "player", displayName(target)));
        return true;
    }

    private boolean handlePayToggle(final CommandSender sender, final String[] args, final int optionIndex) {
        if (!(sender instanceof Player)) {
            messages().send(sender, "<error>Only players can use /paytoggle.");
            return true;
        }
        if (!sender.hasPermission("cofferslegacy.command.paytoggle")) {
            messages().send(sender, "<error>You do not have permission to manage Coffers Legacy payment preferences.");
            return true;
        }

        Boolean desiredState = null;
        if (args.length > optionIndex) {
            String option = args[optionIndex].toLowerCase(Locale.ROOT);
            if ("on".equals(option) || "enable".equals(option) || "enabled".equals(option)) {
                desiredState = Boolean.TRUE;
            } else if ("off".equals(option) || "disable".equals(option) || "disabled".equals(option)) {
                desiredState = Boolean.FALSE;
            } else {
                messages().send(sender, "<usage>Usage<secondary>: <primary>/paytoggle [on|off]");
                return true;
            }
        }

        Player player = (Player) sender;
        boolean paymentsEnabled = desiredState == null
                ? this.plugin.paymentPreferences().togglePayments(player.getUniqueId())
                : this.plugin.paymentPreferences().setAllowsPayments(player.getUniqueId(), desiredState.booleanValue());

        if (paymentsEnabled) {
            messages().send(sender, "<success>You are now accepting Coffers Legacy payments.");
        } else {
            messages().send(sender, "<warning>You are no longer accepting Coffers Legacy payments.");
        }
        return true;
    }

    private boolean handleSet(final CommandSender sender, final String[] args) {
        if (!sender.hasPermission("cofferslegacy.command.set")) {
            messages().send(sender, "<error>You do not have permission to set balances.");
            return true;
        }

        if (args.length < 3) {
            messages().send(sender, "<usage>Usage<secondary>: <primary>/cofferslegacy set <player> <amount> [currency]");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        BigDecimal amount = parseAmount(sender, args[2]);
        if (amount == null) {
            return true;
        }
        String currencyId = args.length >= 4 ? args[3] : this.economy.getDefaultCurrencyId();

        LegacyTransactionResult result = this.economy.setBalance(
                target.getUniqueId(),
                currencyId,
                amount,
                actorFromSender(sender, "command:/cofferslegacy set"),
                "Admin set balance"
        );
        if (!result.isSuccessful()) {
            messages().send(sender, "<error>Balance update failed<secondary>: <primary>%message%", single("message", result.getMessage()));
            return true;
        }
        messages().send(sender, "<success>Set <highlight>%player%<success> to <highlight>%amount%<success>.", mapOf("player", displayName(target), "amount", this.economy.format(result.getCurrencyId(), result.getBalance())));
        return true;
    }

    private boolean handleHistory(final CommandSender sender, final String[] args) {
        if (!sender.hasPermission("cofferslegacy.command.history")) {
            messages().send(sender, "<error>You do not have permission to view Coffers Legacy history.");
            return true;
        }

        OfflinePlayer target;
        int optionStartIndex;
        if (args.length == 1 || looksLikeHistoryOption(args[1])) {
            if (!(sender instanceof Player)) {
                messages().send(sender, "<error>Console must specify a player.");
                return true;
            }
            target = (Player) sender;
            optionStartIndex = 1;
        } else {
            target = Bukkit.getOfflinePlayer(args[1]);
            if (!isSelfTarget(sender, target) && !sender.hasPermission("cofferslegacy.command.history.others")) {
                messages().send(sender, "<error>You do not have permission to view another player's Coffers Legacy history.");
                return true;
            }
            optionStartIndex = 2;
        }

        HistoryOptions options = parseHistoryOptions(args, optionStartIndex);
        List<LegacyLedgerEntry> entries = this.economy.filteredTransactions(target.getUniqueId(), options.offset(), options.getLimit(), options.getCurrencyId(), options.getKind());
        if (entries.isEmpty()) {
            messages().send(sender, "<warning>No Coffers Legacy history exists for <highlight>%player%<warning>.", single("player", displayName(target)));
            return true;
        }

        messages().send(sender, "<accent>Coffers Legacy history for <highlight>%player%<accent> (page <highlight>%page%<accent>, limit <highlight>%limit%<accent>):", mapOf("player", displayName(target), "page", Integer.toString(options.getPage()), "limit", Integer.toString(options.getLimit())));
        for (LegacyLedgerEntry entry : entries) {
            messages().send(sender, "<bullet><primary>%entry%", single("entry", describeEntry(entry)));
        }
        return true;
    }

    private boolean handleCurrencies(final CommandSender sender) {
        if (!sender.hasPermission("cofferslegacy.command.currencies")) {
            messages().send(sender, "<error>You do not have permission to view Coffers Legacy currencies.");
            return true;
        }
        Collection<LegacyCurrencyDefinition> currencies = this.economy.currencies();
        messages().send(sender, "<accent>Available Coffers Legacy currencies:");
        for (LegacyCurrencyDefinition currency : currencies) {
            messages().send(sender, "<bullet><primary>%currency%", single("currency", currency.getId() + " -> " + this.economy.format(currency.getId(), currency.getStartingBalance())));
        }
        return true;
    }

    private boolean handleMigrateVault(final CommandSender sender, final String[] args) {
        if (!sender.hasPermission("cofferslegacy.command.migratevault")) {
            messages().send(sender, "<error>You do not have permission to migrate balances from another Vault economy provider.");
            return true;
        }

        if (!migrationService().available()) {
            messages().send(sender, "<warning>Vault migration is unavailable because Vault is not installed.");
            return true;
        }

        String providerName = args.length >= 2 ? args[1] : null;
        try {
            LegacyMigrationReport report = migrationService().migrate(providerName);
            this.plugin.diagnostics().writeMigrationReport(providerName, report, null, migrationService().availableProviders());
            messages().send(sender, "<success>Migration completed from <highlight>%provider%<success>.", single("provider", report.getProviderName()));
            messages().send(sender, "<bullet><primary>Imported accounts<secondary>: <highlight>%count%", single("count", Integer.toString(report.getImportedAccounts())));
            messages().send(sender, "<bullet><primary>Updated accounts<secondary>: <highlight>%count%", single("count", Integer.toString(report.getUpdatedAccounts())));
            messages().send(sender, "<bullet><primary>Skipped accounts<secondary>: <highlight>%count%", single("count", Integer.toString(report.getSkippedAccounts())));
        } catch (IllegalStateException exception) {
            this.plugin.diagnostics().writeMigrationReport(providerName, null, exception, migrationService().availableProviders());
            messages().send(sender, "<error>Migration failed<secondary>: <primary>%message%", single("message", exception.getMessage()));
            List<String> providers = migrationService().availableProviders();
            if (!providers.isEmpty()) {
                messages().send(sender, "<bullet><primary>Available providers<secondary>: <highlight>%providers%", single("providers", join(providers)));
            }
        }
        return true;
    }

    private boolean handleStatus(final CommandSender sender) {
        if (!sender.hasPermission("cofferslegacy.command.status")) {
            messages().send(sender, "<error>You do not have permission to view Coffers Legacy status.");
            return true;
        }

        String vaultStatus;
        if (this.plugin.isVaultCompatibilityActive()) {
            vaultStatus = "active";
        } else if (!this.plugin.isVaultInstalled()) {
            vaultStatus = "standalone";
        } else if ("disabled".equalsIgnoreCase(this.plugin.configuredVaultBridgeMode())) {
            vaultStatus = "disabled by config";
        } else {
            vaultStatus = "available but inactive";
        }

        messages().send(sender, "<accent>Coffers Legacy status overview:");
        messages().send(sender, "<bullet><primary>Jar line<secondary>: <highlight>legacy");
        messages().send(sender, "<bullet><primary>Storage backend<secondary>: <highlight>%storage%", single("storage", this.plugin.configuredStorageType()));
        messages().send(sender, "<bullet><primary>Default currency<secondary>: <highlight>%currency%", single("currency", this.economy.getDefaultCurrencyId()));
        messages().send(sender, "<bullet><primary>Configured currencies<secondary>: <highlight>%count%", single("count", Integer.toString(this.economy.currencies().size())));
        messages().send(sender, "<bullet><primary>Vault bridge mode<secondary>: <highlight>%mode%", single("mode", this.plugin.configuredVaultBridgeMode()));
        messages().send(sender, "<bullet><primary>Vault runtime state<secondary>: <highlight>%state%", single("state", vaultStatus));
        messages().send(sender, "<bullet><primary>Payments blocked<secondary>: <highlight>%count%", single("count", Integer.toString(this.plugin.paymentPreferences().disabledPaymentCount())));
        messages().send(sender, "<bullet><primary>Registered banks<secondary>: <highlight>%count%", single("count", Integer.toString(this.plugin.bankRegistry().bankCount())));
        messages().send(sender, "<bullet><primary>Latest report folder<secondary>: <highlight>plugins/%plugin%/reports", single("plugin", this.plugin.getDataFolder().getName()));
        return true;
    }

    private boolean handleReload(final CommandSender sender) {
        if (!sender.hasPermission("cofferslegacy.command.reload")) {
            messages().send(sender, "<error>You do not have permission to reload Coffers Legacy.");
            return true;
        }

        if (this.plugin.reloadRuntime()) {
            messages().send(sender, "<success>Reloaded Coffers Legacy successfully.");
        } else {
            messages().send(sender, "<error>Coffers Legacy reload failed. Check the console for config or storage errors.");
        }
        return true;
    }

    private boolean handleTop(final CommandSender sender, final String[] args, final int optionOffset) {
        if (!sender.hasPermission("cofferslegacy.command.top")) {
            messages().send(sender, "<error>You do not have permission to view Coffers Legacy leaderboards.");
            return true;
        }
        String currencyId = this.economy.getDefaultCurrencyId();
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

        List<LegacyAccountSnapshot> topAccounts = this.economy.topAccounts(currencyId, Math.min(limit, 25), excludedBankAccounts());
        messages().send(sender, "<accent>Top Coffers Legacy accounts for <highlight>%currency%<accent>:", single("currency", currencyId));
        if (topAccounts.isEmpty()) {
            messages().send(sender, "<warning>No account data is available yet.");
            return true;
        }

        for (int index = 0; index < topAccounts.size(); index++) {
            LegacyAccountSnapshot snapshot = topAccounts.get(index);
            OfflinePlayer player = Bukkit.getOfflinePlayer(snapshot.getAccountId());
            messages().send(
                    sender,
                    "<bullet><accent>%rank%. <highlight>%player%<secondary> - <primary>%amount%",
                    mapOf(
                            "rank", Integer.toString(index + 1),
                            "player", displayName(player),
                            "amount", this.economy.format(currencyId, snapshot.getBalance())
                    )
            );
        }
        return true;
    }

    private boolean handleBackup(final CommandSender sender, final String[] args) {
        if (!sender.hasPermission("cofferslegacy.command.backup")) {
            messages().send(sender, "<error>You do not have permission to create Coffers Legacy backups.");
            return true;
        }

        String backupName = args.length >= 2 ? args[1] : null;
        try {
            File backupFile = this.plugin.archiveService().backup(
                    backupName,
                    this.plugin.economy().snapshot(),
                    this.plugin.paymentPreferences().disabledPaymentAccounts(),
                    this.plugin.bankRegistry().banks()
            );
            messages().send(sender, "<success>Created Coffers Legacy backup<secondary>: <highlight>%file%", single("file", backupFile.getName()));
        } catch (Exception exception) {
            messages().send(sender, "<error>Backup failed<secondary>: <primary>%message%", single("message", exception.getMessage()));
        }
        return true;
    }

    private boolean handleExport(final CommandSender sender, final String[] args) {
        if (!sender.hasPermission("cofferslegacy.command.export")) {
            messages().send(sender, "<error>You do not have permission to export Coffers Legacy data.");
            return true;
        }

        String exportName = args.length >= 2 ? args[1] : null;
        try {
            File exportFile = this.plugin.archiveService().export(
                    exportName,
                    this.plugin.economy().snapshot(),
                    this.plugin.paymentPreferences().disabledPaymentAccounts(),
                    this.plugin.bankRegistry().banks()
            );
            messages().send(sender, "<success>Exported Coffers Legacy data to <highlight>%file%", single("file", exportFile.getName()));
        } catch (Exception exception) {
            messages().send(sender, "<error>Export failed<secondary>: <primary>%message%", single("message", exception.getMessage()));
        }
        return true;
    }

    private boolean handleImport(final CommandSender sender, final String[] args) {
        if (!sender.hasPermission("cofferslegacy.command.import")) {
            messages().send(sender, "<error>You do not have permission to import Coffers Legacy data.");
            return true;
        }
        if (args.length < 2) {
            messages().send(sender, "<usage>Usage<secondary>: <primary>/cofferslegacy import <export-name>");
            return true;
        }

        try {
            LegacyArchiveSnapshot imported = this.plugin.archiveService().importSnapshot(args[1]);
            this.plugin.economy().replaceSnapshot(imported.getStorageSnapshot());
            this.plugin.paymentPreferences().replaceDisabledPaymentAccounts(imported.getDisabledPaymentAccounts());
            this.plugin.bankRegistry().replaceBanks(imported.getBanks());
            messages().send(sender, "<success>Imported Coffers Legacy data from <highlight>%name%<success>.", single("name", args[1]));
        } catch (Exception exception) {
            messages().send(sender, "<error>Import failed<secondary>: <primary>%message%", single("message", exception.getMessage()));
        }
        return true;
    }

    private boolean handleRestore(final CommandSender sender, final String[] args) {
        if (!sender.hasPermission("cofferslegacy.command.restore")) {
            messages().send(sender, "<error>You do not have permission to restore Coffers Legacy backups.");
            return true;
        }

        String backupName = args.length >= 2 ? args[1] : "latest";
        try {
            LegacyArchiveSnapshot restored = this.plugin.archiveService().restoreBackup(backupName);
            this.plugin.economy().replaceSnapshot(restored.getStorageSnapshot());
            this.plugin.paymentPreferences().replaceDisabledPaymentAccounts(restored.getDisabledPaymentAccounts());
            this.plugin.bankRegistry().replaceBanks(restored.getBanks());
            messages().send(
                    sender,
                    "<success>Restored Coffers Legacy backup <highlight>%name%<success> without restarting the server.",
                    single("name", backupName)
            );
        } catch (Exception exception) {
            messages().send(sender, "<error>Backup restore failed<secondary>: <primary>%message%", single("message", exception.getMessage()));
        }
        return true;
    }

    private boolean handleRollback(final CommandSender sender, final String[] args) {
        if (!sender.hasPermission("cofferslegacy.command.rollback")) {
            messages().send(sender, "<error>You do not have permission to roll back Coffers Legacy transactions.");
            return true;
        }
        if (args.length < 2) {
            messages().send(sender, "<usage>Usage<secondary>: <primary>/cofferslegacy rollback <entry-id>");
            return true;
        }

        UUID entryId;
        try {
            entryId = UUID.fromString(args[1]);
        } catch (IllegalArgumentException exception) {
            messages().send(sender, "<error>That transaction id is not a valid UUID.");
            return true;
        }

        LegacyTransactionResult result = this.economy.rollback(
                entryId,
                actorFromSender(sender, "command:/cofferslegacy rollback"),
                "Rollback requested by " + sender.getName()
        );
        if (!result.isSuccessful()) {
            messages().send(sender, "<error>Rollback failed<secondary>: <primary>%message%", single("message", result.getMessage()));
            return true;
        }

        messages().send(sender, "<success>Rolled back transaction <highlight>%entry%<success>. New balance: <highlight>%balance%", mapOf("entry", entryId.toString(), "balance", this.economy.format(result.getCurrencyId(), result.getBalance())));
        return true;
    }

    private boolean handleBank(final CommandSender sender, final String[] args) {
        if (args.length < 3) {
            sendBankUsage(sender);
            return true;
        }

        String action = args[1].toLowerCase(Locale.ROOT);
        String bankName = args[2];
        if ("create".equals(action)) {
            return handleBankCreate(sender, bankName);
        }
        if ("delete".equals(action) || "remove".equals(action)) {
            return handleBankDelete(sender, bankName);
        }
        if ("balance".equals(action)) {
            return handleBankBalance(sender, args, bankName);
        }
        if ("deposit".equals(action)) {
            return handleBankDeposit(sender, args, bankName);
        }
        if ("withdraw".equals(action)) {
            return handleBankWithdraw(sender, args, bankName);
        }
        if ("history".equals(action)) {
            return handleBankHistory(sender, args, bankName);
        }

        sendBankUsage(sender);
        return true;
    }

    private BigDecimal parseAmount(final CommandSender sender, final String raw) {
        try {
            BigDecimal amount = new BigDecimal(raw);
            if (amount.signum() < 0) {
                messages().send(sender, "<error>Amount must not be negative.");
                return null;
            }
            return amount;
        } catch (NumberFormatException exception) {
            messages().send(sender, "<error>Invalid amount<secondary>: <primary>%amount%", single("amount", raw));
            return null;
        }
    }

    private boolean handleBankCreate(final CommandSender sender, final String bankName) {
        if (!sender.hasPermission("cofferslegacy.command.bank.create")) {
            messages().send(sender, "<error>You do not have permission to create Coffers Legacy banks.");
            return true;
        }
        if (!this.plugin.bankRegistry().createBank(bankName)) {
            messages().send(sender, "<warning>That Coffers Legacy bank already exists or the name is invalid.");
            return true;
        }
        this.economy.createAccount(this.plugin.bankRegistry().bankAccountId(bankName));
        messages().send(sender, "<success>Created Coffers Legacy bank <highlight>%bank%<success>.", single("bank", bankName));
        return true;
    }

    private boolean handleBankDelete(final CommandSender sender, final String bankName) {
        if (!sender.hasPermission("cofferslegacy.command.bank.delete")) {
            messages().send(sender, "<error>You do not have permission to delete Coffers Legacy banks.");
            return true;
        }
        if (!this.plugin.bankRegistry().exists(bankName)) {
            messages().send(sender, "<warning>No Coffers Legacy bank exists with that name.");
            return true;
        }
        UUID bankId = this.plugin.bankRegistry().bankAccountId(bankName);
        this.economy.purgeAccount(bankId);
        this.plugin.bankRegistry().deleteBank(bankName);
        messages().send(sender, "<success>Deleted Coffers Legacy bank <highlight>%bank%<success>.", single("bank", bankName));
        return true;
    }

    private boolean handleBankBalance(final CommandSender sender, final String[] args, final String bankName) {
        if (!sender.hasPermission("cofferslegacy.command.bank.balance")) {
            messages().send(sender, "<error>You do not have permission to view Coffers Legacy bank balances.");
            return true;
        }
        UUID bankId = bankId(bankName, sender);
        if (bankId == null) {
            return true;
        }
        String currencyId = args.length >= 4 ? args[3] : this.economy.getDefaultCurrencyId();
        BigDecimal balance = this.economy.getBalance(bankId, currencyId);
        messages().send(sender, "<success>Bank balance for <highlight>%bank%<success>: <highlight>%balance%", mapOf("bank", bankName, "balance", this.economy.format(currencyId, balance)));
        return true;
    }

    private boolean handleBankDeposit(final CommandSender sender, final String[] args, final String bankName) {
        if (!sender.hasPermission("cofferslegacy.command.bank.deposit")) {
            messages().send(sender, "<error>You do not have permission to deposit into Coffers Legacy banks.");
            return true;
        }
        if (args.length < 4) {
            messages().send(sender, "<usage>Usage<secondary>: <primary>/cofferslegacy bank deposit <name> <amount> [currency]");
            return true;
        }
        UUID bankId = bankId(bankName, sender);
        if (bankId == null) {
            return true;
        }
        BigDecimal amount = parseAmount(sender, args[3]);
        if (amount == null) {
            return true;
        }
        String currencyId = args.length >= 5 ? args[4] : this.economy.getDefaultCurrencyId();
        LegacyTransactionResult result = this.economy.deposit(bankId, currencyId, amount, actorFromSender(sender, "command:/cofferslegacy bank deposit"), "Bank deposit");
        if (!result.isSuccessful()) {
            messages().send(sender, "<error>Bank deposit failed<secondary>: <primary>%message%", single("message", result.getMessage()));
            return true;
        }
        messages().send(sender, "<success>Deposited <highlight>%amount%<success> into bank <highlight>%bank%<success>.", mapOf("amount", this.economy.format(currencyId, amount), "bank", bankName));
        return true;
    }

    private boolean handleBankWithdraw(final CommandSender sender, final String[] args, final String bankName) {
        if (!sender.hasPermission("cofferslegacy.command.bank.withdraw")) {
            messages().send(sender, "<error>You do not have permission to withdraw from Coffers Legacy banks.");
            return true;
        }
        if (args.length < 4) {
            messages().send(sender, "<usage>Usage<secondary>: <primary>/cofferslegacy bank withdraw <name> <amount> [currency]");
            return true;
        }
        UUID bankId = bankId(bankName, sender);
        if (bankId == null) {
            return true;
        }
        BigDecimal amount = parseAmount(sender, args[3]);
        if (amount == null) {
            return true;
        }
        String currencyId = args.length >= 5 ? args[4] : this.economy.getDefaultCurrencyId();
        LegacyTransactionResult result = this.economy.withdraw(bankId, currencyId, amount, actorFromSender(sender, "command:/cofferslegacy bank withdraw"), "Bank withdrawal");
        if (!result.isSuccessful()) {
            messages().send(sender, "<error>Bank withdrawal failed<secondary>: <primary>%message%", single("message", result.getMessage()));
            return true;
        }
        messages().send(sender, "<success>Withdrew <highlight>%amount%<success> from bank <highlight>%bank%<success>.", mapOf("amount", this.economy.format(currencyId, amount), "bank", bankName));
        return true;
    }

    private boolean handleBankHistory(final CommandSender sender, final String[] args, final String bankName) {
        if (!sender.hasPermission("cofferslegacy.command.bank.history")) {
            messages().send(sender, "<error>You do not have permission to view Coffers Legacy bank history.");
            return true;
        }
        UUID bankId = bankId(bankName, sender);
        if (bankId == null) {
            return true;
        }
        HistoryOptions options = parseHistoryOptions(args, 3);
        List<LegacyLedgerEntry> entries = this.economy.filteredTransactions(bankId, options.offset(), options.getLimit(), options.getCurrencyId(), options.getKind());
        if (entries.isEmpty()) {
            messages().send(sender, "<warning>No Coffers Legacy bank history exists for <highlight>%bank%<warning>.", single("bank", bankName));
            return true;
        }
        messages().send(sender, "<accent>Coffers Legacy bank history for <highlight>%bank%<accent>:", single("bank", bankName));
        for (LegacyLedgerEntry entry : entries) {
            messages().send(sender, "<bullet><primary>%entry%", single("entry", describeEntry(entry)));
        }
        return true;
    }

    private UUID bankId(final String bankName, final CommandSender sender) {
        if (!this.plugin.bankRegistry().exists(bankName)) {
            messages().send(sender, "<warning>No Coffers Legacy bank exists with that name.");
            return null;
        }
        return this.plugin.bankRegistry().bankAccountId(bankName);
    }

    private void sendBankUsage(final CommandSender sender) {
        messages().send(sender, "<accent>Coffers Legacy bank commands:");
        messages().send(sender, "<bullet><usage>/cofferslegacy bank create <name>");
        messages().send(sender, "<bullet><usage>/cofferslegacy bank delete <name>");
        messages().send(sender, "<bullet><usage>/cofferslegacy bank balance <name> [currency]");
        messages().send(sender, "<bullet><usage>/cofferslegacy bank deposit <name> <amount> [currency]");
        messages().send(sender, "<bullet><usage>/cofferslegacy bank withdraw <name> <amount> [currency]");
        messages().send(sender, "<bullet><usage>/cofferslegacy bank history <name> [page=<n>] [limit=<n>] [currency=<id>] [kind=<kind>]");
    }

    private HistoryOptions parseHistoryOptions(final String[] args, final int startIndex) {
        int page = 1;
        int limit = 5;
        String currencyId = null;
        LegacyTransactionKind kind = null;
        for (int index = startIndex; index < args.length; index++) {
            String token = args[index];
            if (token.startsWith("page=")) {
                page = Math.max(1, parseLimit(token.substring("page=".length())));
            } else if (token.startsWith("limit=")) {
                limit = Math.max(1, parseLimit(token.substring("limit=".length())));
            } else if (token.startsWith("currency=")) {
                currencyId = token.substring("currency=".length());
            } else if (token.startsWith("kind=") || token.startsWith("type=")) {
                kind = LegacyTransactionKind.valueOf(token.substring(token.indexOf('=') + 1).toUpperCase(Locale.ROOT));
            } else if (isInteger(token)) {
                limit = Math.max(1, parseLimit(token));
            } else if (currencyId == null && isCurrencyId(token)) {
                currencyId = token;
            }
        }
        return new HistoryOptions(page, limit, currencyId, kind);
    }

    private boolean looksLikeHistoryOption(final String token) {
        return token.indexOf('=') >= 0 || isInteger(token)
                || "deposit".equalsIgnoreCase(token)
                || "withdrawal".equalsIgnoreCase(token)
                || "transfer_in".equalsIgnoreCase(token)
                || "transfer_out".equalsIgnoreCase(token)
                || "set".equalsIgnoreCase(token);
    }

    private String displayName(final OfflinePlayer player) {
        String bankName = this.plugin.bankRegistry().displayName(player.getUniqueId());
        if (bankName != null) {
            return "[Bank] " + bankName;
        }
        return player.getName() != null ? player.getName() : player.getUniqueId().toString();
    }

    private void sendUsage(final CommandSender sender) {
        messages().send(sender, "<accent>Coffers Legacy commands:");
        messages().send(sender, "<bullet><usage>/cofferslegacy balance [player] [currency]");
        messages().send(sender, "<bullet><usage>/cofferslegacy pay <player> <amount> [currency]");
        messages().send(sender, "<bullet><usage>/paytoggle [on|off]");
        messages().send(sender, "<bullet><usage>/cofferslegacy set <player> <amount> [currency]");
        messages().send(sender, "<bullet><usage>/cofferslegacy history [player] [limit]");
        messages().send(sender, "<bullet><usage>/cofferslegacy rollback <entry-id>");
        messages().send(sender, "<bullet><usage>/cofferslegacy bank <create|delete|balance|deposit|withdraw|history> ...");
        messages().send(sender, "<bullet><usage>/cofferslegacy status");
        messages().send(sender, "<bullet><usage>/baltop [currency] [limit]");
        messages().send(sender, "<bullet><usage>/cofferslegacy currencies");
        messages().send(sender, "<bullet><usage>/cofferslegacy migratevault [provider]");
        messages().send(sender, "<bullet><usage>/cofferslegacy reload");
        messages().send(sender, "<bullet><usage>/cofferslegacy backup [name]");
        messages().send(sender, "<bullet><usage>/cofferslegacy export [name]");
        messages().send(sender, "<bullet><usage>/cofferslegacy import <export-name>");
        messages().send(sender, "<bullet><usage>/cofferslegacy restore [backup-name|latest]");
    }

    public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
        String commandName = command.getName().toLowerCase(Locale.ROOT);
        if ("baltop".equals(commandName)) {
            if (args.length == 1) {
                return filter(currencyIds(), args[0]);
            }
            return Collections.emptyList();
        }
        if ("paytoggle".equals(commandName)) {
            if (args.length == 1) {
                return filter(Arrays.asList("on", "off"), args[0]);
            }
            return Collections.emptyList();
        }

        List<String> completions = new ArrayList<String>();

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
            if (sender.hasPermission("cofferslegacy.command.set")) {
                completions.add("set");
            }
            if (sender.hasPermission("cofferslegacy.command.migratevault")) {
                completions.add("migratevault");
            }
            if (sender.hasPermission("cofferslegacy.command.reload")) {
                completions.add("reload");
            }
            if (sender.hasPermission("cofferslegacy.command.backup")) {
                completions.add("backup");
            }
            if (sender.hasPermission("cofferslegacy.command.export")) {
                completions.add("export");
            }
            if (sender.hasPermission("cofferslegacy.command.import")) {
                completions.add("import");
            }
            if (sender.hasPermission("cofferslegacy.command.restore")) {
                completions.add("restore");
            }
            return filter(completions, args[0]);
        }

        if (args.length == 2 && Arrays.asList("balance", "pay", "set", "history").contains(args[0].toLowerCase(Locale.ROOT))) {
            for (Player online : this.plugin.getServer().getOnlinePlayers()) {
                completions.add(online.getName());
            }
            return filter(completions, args[1]);
        }

        if (args.length == 2 && Arrays.asList("top", "baltop").contains(args[0].toLowerCase(Locale.ROOT))) {
            return filter(currencyIds(), args[1]);
        }

        if ((args.length == 3 && "balance".equalsIgnoreCase(args[0]))
                || (args.length == 4 && Arrays.asList("pay", "set").contains(args[0].toLowerCase(Locale.ROOT)))) {
            return filter(currencyIds(), args[args.length - 1]);
        }

        if (args.length == 2 && "paytoggle".equalsIgnoreCase(args[0])) {
            return filter(Arrays.asList("on", "off"), args[1]);
        }

        if (args.length == 2 && "migratevault".equalsIgnoreCase(args[0])) {
            if (!migrationService().available()) {
                return Collections.emptyList();
            }
            return filter(migrationService().availableProviders(), args[1]);
        }

        if (args.length == 2 && "restore".equalsIgnoreCase(args[0])) {
            return filter(Arrays.asList("latest"), args[1]);
        }

        if (args.length == 2 && "bank".equalsIgnoreCase(args[0])) {
            return filter(Arrays.asList("create", "delete", "balance", "deposit", "withdraw", "history"), args[1]);
        }

        if (args.length == 3 && "bank".equalsIgnoreCase(args[0]) && !"create".equalsIgnoreCase(args[1])) {
            return filter(new ArrayList<String>(this.plugin.bankRegistry().banks()), args[2]);
        }

        return Collections.emptyList();
    }

    private List<String> currencyIds() {
        List<String> ids = new ArrayList<String>();
        for (LegacyCurrencyDefinition currency : this.economy.currencies()) {
            ids.add(currency.getId());
        }
        return ids;
    }

    private boolean isCurrencyId(final String currencyId) {
        for (LegacyCurrencyDefinition currency : this.economy.currencies()) {
            if (currency.getId().equalsIgnoreCase(currencyId)) {
                return true;
            }
        }
        return false;
    }

    private List<String> filter(final List<String> candidates, final String token) {
        List<String> matches = new ArrayList<String>();
        String needle = token.toLowerCase(Locale.ROOT);
        for (String candidate : candidates) {
            if (candidate.toLowerCase(Locale.ROOT).startsWith(needle)) {
                matches.add(candidate);
            }
        }
        return matches;
    }

    private int parseLimit(final String raw) {
        try {
            return Math.max(1, Integer.parseInt(raw));
        } catch (NumberFormatException exception) {
            return 5;
        }
    }

    private LegacyTransactionActor actorFromSender(final CommandSender sender, final String source) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            return LegacyTransactionActor.player(player.getUniqueId(), player.getName(), source);
        }
        return LegacyTransactionActor.console(source);
    }

    private String describeEntry(final LegacyLedgerEntry entry) {
        String action;
        switch (entry.getKind()) {
            case DEPOSIT:
                action = "deposit";
                break;
            case WITHDRAWAL:
                action = "withdrawal";
                break;
            case TRANSFER_IN:
                action = "transfer in";
                break;
            case TRANSFER_OUT:
                action = "transfer out";
                break;
            case SET:
            default:
                action = "set balance";
                break;
        }

        StringBuilder builder = new StringBuilder();
        builder.append(action)
                .append(" ")
                .append(this.economy.format(entry.getCurrencyId(), entry.getAmount()))
                .append(" | previous ")
                .append(this.economy.format(entry.getCurrencyId(), entry.getPreviousBalance()))
                .append(" | balance ")
                .append(this.economy.format(entry.getCurrencyId(), entry.getResultingBalance()));
        UUID counterparty = entry.getCounterpartyAccountId();
        if (counterparty != null
                && (entry.getKind() == LegacyTransactionKind.TRANSFER_IN || entry.getKind() == LegacyTransactionKind.TRANSFER_OUT)) {
            builder.append(" | other account ").append(accountLabel(counterparty));
        }
        if (entry.getReason() != null && !entry.getReason().trim().isEmpty()) {
            builder.append(" | ").append(entry.getReason());
        }
        if (entry.getActor() != null && entry.getActor().getActorName() != null) {
            builder.append(" | by ").append(entry.getActor().getActorName());
        }
        if (entry.getReversalOfReferenceId() != null) {
            builder.append(" | rollback of ").append(entry.getReversalOfReferenceId().toString());
        }
        return builder.toString();
    }

    private String accountLabel(final UUID accountId) {
        String bankName = this.plugin.bankRegistry().displayName(accountId);
        if (bankName != null) {
            return "[Bank] " + bankName;
        }
        return displayName(Bukkit.getOfflinePlayer(accountId));
    }

    private java.util.Set<UUID> excludedBankAccounts() {
        java.util.Set<UUID> accountIds = new java.util.LinkedHashSet<UUID>();
        for (String bank : this.plugin.bankRegistry().banks()) {
            accountIds.add(this.plugin.bankRegistry().bankAccountId(bank));
        }
        return accountIds;
    }

    private String join(final List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                builder.append(", ");
            }
            builder.append(values.get(index));
        }
        return builder.toString();
    }

    private LegacyMigrationGateway migrationService() {
        return this.plugin.migrationService();
    }

    private LegacyMessagePalette messages() {
        return this.plugin.messages();
    }

    private boolean isInteger(final String raw) {
        try {
            Integer.parseInt(raw);
            return true;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private boolean isSelfTarget(final CommandSender sender, final OfflinePlayer target) {
        return sender instanceof Player && ((Player) sender).getUniqueId().equals(target.getUniqueId());
    }

    private static final class HistoryOptions {
        private final int page;
        private final int limit;
        private final String currencyId;
        private final LegacyTransactionKind kind;

        private HistoryOptions(final int page, final int limit, final String currencyId, final LegacyTransactionKind kind) {
            this.page = page;
            this.limit = limit;
            this.currencyId = currencyId;
            this.kind = kind;
        }

        private int getPage() {
            return this.page;
        }

        private int getLimit() {
            return this.limit;
        }

        private String getCurrencyId() {
            return this.currencyId;
        }

        private LegacyTransactionKind getKind() {
            return this.kind;
        }

        private int offset() {
            return Math.max(0, (this.page - 1) * this.limit);
        }
    }

    private java.util.Map<String, String> single(final String key, final String value) {
        java.util.Map<String, String> placeholders = new java.util.LinkedHashMap<String, String>();
        placeholders.put(key, value);
        return placeholders;
    }

    private java.util.Map<String, String> mapOf(final String key1, final String value1, final String key2, final String value2) {
        java.util.Map<String, String> placeholders = new java.util.LinkedHashMap<String, String>();
        placeholders.put(key1, value1);
        placeholders.put(key2, value2);
        return placeholders;
    }

    private java.util.Map<String, String> mapOf(final String key1, final String value1, final String key2, final String value2, final String key3, final String value3) {
        java.util.Map<String, String> placeholders = new java.util.LinkedHashMap<String, String>();
        placeholders.put(key1, value1);
        placeholders.put(key2, value2);
        placeholders.put(key3, value3);
        return placeholders;
    }
}
