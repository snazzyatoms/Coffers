package com.aegisguard.coffers.legacy;

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

final class CoffersLegacyCommand implements CommandExecutor, TabCompleter {

    private final CoffersLegacyPlugin plugin;
    private final CoffersLegacyEconomyService economy;
    private final CoffersLegacyMigrationService migrationService;

    CoffersLegacyCommand(
            final CoffersLegacyPlugin plugin,
            final CoffersLegacyEconomyService economy,
            final CoffersLegacyMigrationService migrationService
    ) {
        this.plugin = plugin;
        this.economy = economy;
        this.migrationService = migrationService;
    }

    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
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
            if ("set".equals(action)) {
                return handleSet(sender, args);
            }
            if ("history".equals(action)) {
                return handleHistory(sender, args);
            }
            if ("currencies".equals(action)) {
                return handleCurrencies(sender);
            }
            if ("migratevault".equals(action)) {
                return handleMigrateVault(sender, args);
            }
        } catch (IllegalArgumentException exception) {
            sender.sendMessage("Coffers Legacy error: " + exception.getMessage());
            return true;
        } catch (IllegalStateException exception) {
            sender.sendMessage("Coffers Legacy failed to complete that request: " + exception.getMessage());
            return true;
        }

        sendUsage(sender);
        return true;
    }

    private boolean handleBalance(final CommandSender sender, final String[] args) {
        if (args.length == 1) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Console must specify a player.");
                return true;
            }

            Player player = (Player) sender;
            String currencyId = this.economy.getDefaultCurrencyId();
            BigDecimal balance = this.economy.getBalance(player.getUniqueId(), currencyId);
            sender.sendMessage("Balance: " + this.economy.format(currencyId, balance));
            return true;
        }

        if (args.length == 2 && sender instanceof Player && isCurrencyId(args[1])) {
            Player player = (Player) sender;
            String currencyId = args[1];
            BigDecimal balance = this.economy.getBalance(player.getUniqueId(), currencyId);
            sender.sendMessage("Balance: " + this.economy.format(currencyId, balance));
            return true;
        }

        if (!sender.hasPermission("cofferslegacy.command.balance.others")) {
            sender.sendMessage("You do not have permission to view another player's balance.");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        String currencyId = args.length >= 3 ? args[2] : this.economy.getDefaultCurrencyId();
        BigDecimal balance = this.economy.getBalance(target.getUniqueId(), currencyId);
        sender.sendMessage(displayName(target) + " has " + this.economy.format(currencyId, balance));
        return true;
    }

    private boolean handlePay(final CommandSender sender, final String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use /cofferslegacy pay.");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage("Usage: /cofferslegacy pay <player> <amount> [currency]");
            return true;
        }

        Player player = (Player) sender;
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (player.getUniqueId().equals(target.getUniqueId())) {
            sender.sendMessage("You cannot pay yourself.");
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
            sender.sendMessage("Payment failed: " + result.getMessage());
            return true;
        }

        sender.sendMessage("Sent " + this.economy.format(result.getCurrencyId(), result.getAmount()) + " to " + displayName(target) + ".");
        return true;
    }

    private boolean handleSet(final CommandSender sender, final String[] args) {
        if (!sender.hasPermission("cofferslegacy.command.set")) {
            sender.sendMessage("You do not have permission to set balances.");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage("Usage: /cofferslegacy set <player> <amount> [currency]");
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
            sender.sendMessage("Balance update failed: " + result.getMessage());
            return true;
        }
        sender.sendMessage("Set " + displayName(target) + " to " + this.economy.format(result.getCurrencyId(), result.getBalance()) + ".");
        return true;
    }

    private boolean handleHistory(final CommandSender sender, final String[] args) {
        OfflinePlayer target;
        int limit;

        if (args.length == 1) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Console must specify a player.");
                return true;
            }
            target = (Player) sender;
            limit = 5;
        } else {
            target = Bukkit.getOfflinePlayer(args[1]);
            limit = args.length >= 3 ? parseLimit(args[2]) : 5;
        }

        List<LegacyLedgerEntry> entries = this.economy.recentTransactions(target.getUniqueId(), limit);
        if (entries.isEmpty()) {
            sender.sendMessage("No Coffers Legacy history exists for " + displayName(target) + ".");
            return true;
        }

        sender.sendMessage("Recent Coffers Legacy history for " + displayName(target) + ":");
        for (LegacyLedgerEntry entry : entries) {
            sender.sendMessage("- " + describeEntry(entry));
        }
        return true;
    }

    private boolean handleCurrencies(final CommandSender sender) {
        Collection<LegacyCurrencyDefinition> currencies = this.economy.currencies();
        sender.sendMessage("Available Coffers Legacy currencies:");
        for (LegacyCurrencyDefinition currency : currencies) {
            sender.sendMessage("- " + currency.getId() + " -> " + this.economy.format(currency.getId(), currency.getStartingBalance()));
        }
        return true;
    }

    private boolean handleMigrateVault(final CommandSender sender, final String[] args) {
        if (!sender.hasPermission("cofferslegacy.command.migratevault")) {
            sender.sendMessage("You do not have permission to migrate balances from another Vault economy provider.");
            return true;
        }

        String providerName = args.length >= 2 ? args[1] : null;
        try {
            LegacyMigrationReport report = this.migrationService.migrate(providerName);
            sender.sendMessage("Migration completed from " + report.getProviderName() + ".");
            sender.sendMessage("Imported accounts: " + report.getImportedAccounts());
            sender.sendMessage("Updated accounts: " + report.getUpdatedAccounts());
            sender.sendMessage("Skipped accounts: " + report.getSkippedAccounts());
        } catch (IllegalStateException exception) {
            sender.sendMessage("Migration failed: " + exception.getMessage());
            List<String> providers = this.migrationService.availableProviders();
            if (!providers.isEmpty()) {
                sender.sendMessage("Available providers: " + join(providers));
            }
        }
        return true;
    }

    private BigDecimal parseAmount(final CommandSender sender, final String raw) {
        try {
            BigDecimal amount = new BigDecimal(raw);
            if (amount.signum() < 0) {
                sender.sendMessage("Amount must not be negative.");
                return null;
            }
            return amount;
        } catch (NumberFormatException exception) {
            sender.sendMessage("Invalid amount: " + raw);
            return null;
        }
    }

    private String displayName(final OfflinePlayer player) {
        return player.getName() != null ? player.getName() : player.getUniqueId().toString();
    }

    private void sendUsage(final CommandSender sender) {
        sender.sendMessage("Coffers Legacy commands:");
        sender.sendMessage("/cofferslegacy balance [player] [currency]");
        sender.sendMessage("/cofferslegacy pay <player> <amount> [currency]");
        sender.sendMessage("/cofferslegacy set <player> <amount> [currency]");
        sender.sendMessage("/cofferslegacy history [player] [limit]");
        sender.sendMessage("/cofferslegacy currencies");
        sender.sendMessage("/cofferslegacy migratevault [provider]");
    }

    public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
        List<String> completions = new ArrayList<String>();

        if (args.length == 1) {
            completions.add("balance");
            completions.add("pay");
            completions.add("history");
            completions.add("currencies");
            if (sender.hasPermission("cofferslegacy.command.set")) {
                completions.add("set");
            }
            if (sender.hasPermission("cofferslegacy.command.migratevault")) {
                completions.add("migratevault");
            }
            return filter(completions, args[0]);
        }

        if (args.length == 2 && Arrays.asList("balance", "pay", "set", "history").contains(args[0].toLowerCase(Locale.ROOT))) {
            for (Player online : this.plugin.getServer().getOnlinePlayers()) {
                completions.add(online.getName());
            }
            return filter(completions, args[1]);
        }

        if ((args.length == 3 && "balance".equalsIgnoreCase(args[0]))
                || (args.length == 4 && Arrays.asList("pay", "set").contains(args[0].toLowerCase(Locale.ROOT)))) {
            return filter(currencyIds(), args[args.length - 1]);
        }

        if (args.length == 2 && "migratevault".equalsIgnoreCase(args[0])) {
            return filter(this.migrationService.availableProviders(), args[1]);
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
                .append(" | balance ")
                .append(this.economy.format(entry.getCurrencyId(), entry.getResultingBalance()));
        UUID counterparty = entry.getCounterpartyAccountId();
        if (counterparty != null
                && (entry.getKind() == LegacyTransactionKind.TRANSFER_IN || entry.getKind() == LegacyTransactionKind.TRANSFER_OUT)) {
            builder.append(" | other account ").append(counterparty.toString());
        }
        if (entry.getReason() != null && !entry.getReason().trim().isEmpty()) {
            builder.append(" | ").append(entry.getReason());
        }
        if (entry.getActor() != null && entry.getActor().getActorName() != null) {
            builder.append(" | by ").append(entry.getActor().getActorName());
        }
        return builder.toString();
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
}
