package com.aegisguard.coffers.paper;

import com.aegisguard.coffers.api.CoffersEconomy;
import com.aegisguard.coffers.api.LedgerEntry;
import com.aegisguard.coffers.api.TransactionActor;
import com.aegisguard.coffers.api.TransactionKind;
import com.aegisguard.coffers.api.TransactionResult;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

final class CoffersCommand implements CommandExecutor, TabCompleter {

    private final CoffersPlugin plugin;
    private final CoffersEconomy economy;
    private final VaultMigrationService migrationService;

    CoffersCommand(final CoffersPlugin plugin, final CoffersEconomy economy, final VaultMigrationService migrationService) {
        this.plugin = plugin;
        this.economy = economy;
        this.migrationService = migrationService;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        try {
            return switch (args[0].toLowerCase(Locale.ROOT)) {
                case "balance" -> handleBalance(sender, args);
                case "pay" -> handlePay(sender, args);
                case "set" -> handleSet(sender, args);
                case "history" -> handleHistory(sender, args);
                case "currencies" -> handleCurrencies(sender);
                case "migratevault" -> handleMigrateVault(sender, args);
                default -> {
                    sendUsage(sender);
                    yield true;
                }
            };
        } catch (final IllegalArgumentException exception) {
            sender.sendMessage("Coffers error: " + exception.getMessage());
            return true;
        } catch (final IllegalStateException exception) {
            sender.sendMessage("Coffers failed to complete that request: " + exception.getMessage());
            return true;
        }
    }

    private boolean handleBalance(final CommandSender sender, final String[] args) {
        if (args.length == 1) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Console must specify a player.");
                return true;
            }

            final String currencyId = args.length >= 2 ? args[1] : this.economy.defaultCurrencyId();
            final BigDecimal balance = this.economy.getBalance(player.getUniqueId(), currencyId);
            sender.sendMessage("Balance: " + this.economy.format(currencyId, balance));
            return true;
        }

        if (args.length == 2 && sender instanceof Player player && this.economy.currency(args[1]).isPresent()) {
            final String currencyId = args[1];
            final BigDecimal balance = this.economy.getBalance(player.getUniqueId(), currencyId);
            sender.sendMessage("Balance: " + this.economy.format(currencyId, balance));
            return true;
        }

        if (!sender.hasPermission("coffers.command.balance.others")) {
            sender.sendMessage("You do not have permission to view another player's balance.");
            return true;
        }

        final OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        final String currencyId = args.length >= 3 ? args[2] : this.economy.defaultCurrencyId();
        final BigDecimal balance = this.economy.getBalance(target.getUniqueId(), currencyId);
        sender.sendMessage(displayName(target) + " has " + this.economy.format(currencyId, balance));
        return true;
    }

    private boolean handlePay(final CommandSender sender, final String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use /coffers pay.");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage("Usage: /coffers pay <player> <amount> [currency]");
            return true;
        }

        final OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (Objects.equals(target.getUniqueId(), player.getUniqueId())) {
            sender.sendMessage("You cannot pay yourself.");
            return true;
        }

        final BigDecimal amount = parseAmount(sender, args[2]);
        if (amount == null) {
            return true;
        }
        final String currencyId = args.length >= 4 ? args[3] : this.economy.defaultCurrencyId();

        final TransactionResult result = this.economy.transfer(
                player.getUniqueId(),
                target.getUniqueId(),
                currencyId,
                amount,
                TransactionActor.player(player.getUniqueId(), player.getName(), "command:/coffers pay"),
                "Player transfer"
        );

        if (!result.successful()) {
            sender.sendMessage("Payment failed: " + result.message());
            return true;
        }

        sender.sendMessage("Sent " + this.economy.format(result.currencyId(), result.amount()) + " to " + displayName(target) + ".");
        return true;
    }

    private boolean handleSet(final CommandSender sender, final String[] args) {
        if (!sender.hasPermission("coffers.command.set")) {
            sender.sendMessage("You do not have permission to set balances.");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage("Usage: /coffers set <player> <amount> [currency]");
            return true;
        }

        final OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        final BigDecimal amount = parseAmount(sender, args[2]);
        if (amount == null) {
            return true;
        }
        final String currencyId = args.length >= 4 ? args[3] : this.economy.defaultCurrencyId();

        final TransactionResult result = this.economy.setBalance(
                target.getUniqueId(),
                currencyId,
                amount,
                actorFromSender(sender, "command:/coffers set"),
                "Admin set balance"
        );
        if (!result.successful()) {
            sender.sendMessage("Balance update failed: " + result.message());
            return true;
        }
        sender.sendMessage("Set " + displayName(target) + " to " + this.economy.format(result.currencyId(), result.balance()) + ".");
        return true;
    }

    private boolean handleHistory(final CommandSender sender, final String[] args) {
        final OfflinePlayer target;
        final int limit;

        if (args.length == 1) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Console must specify a player.");
                return true;
            }
            target = player;
            limit = 5;
        } else {
            target = Bukkit.getOfflinePlayer(args[1]);
            limit = args.length >= 3 ? parseLimit(args[2]) : 5;
        }

        final List<LedgerEntry> entries = this.economy.recentTransactions(target.getUniqueId(), limit);
        if (entries.isEmpty()) {
            sender.sendMessage("No Coffers history exists for " + displayName(target) + ".");
            return true;
        }

        sender.sendMessage("Recent Coffers history for " + displayName(target) + ":");
        for (final LedgerEntry entry : entries) {
            sender.sendMessage("- " + describeEntry(entry));
        }
        return true;
    }

    private boolean handleCurrencies(final CommandSender sender) {
        final Collection<String> descriptions = this.economy.currencies().stream()
                .map(currency -> currency.id() + " -> " + this.economy.format(currency.id(), currency.startingBalance()))
                .toList();
        sender.sendMessage("Available Coffers currencies:");
        for (final String description : descriptions) {
            sender.sendMessage("- " + description);
        }
        return true;
    }

    private boolean handleMigrateVault(final CommandSender sender, final String[] args) {
        if (!sender.hasPermission("coffers.command.migratevault")) {
            sender.sendMessage("You do not have permission to migrate balances from another Vault economy provider.");
            return true;
        }

        final String providerName = args.length >= 2 ? args[1] : null;
        try {
            final MigrationReport report = this.migrationService.migrate(providerName);
            sender.sendMessage("Migration completed from " + report.providerName() + ".");
            sender.sendMessage("Imported accounts: " + report.importedAccounts());
            sender.sendMessage("Updated accounts: " + report.updatedAccounts());
            sender.sendMessage("Skipped accounts: " + report.skippedAccounts());
        } catch (final IllegalStateException exception) {
            sender.sendMessage("Migration failed: " + exception.getMessage());
            final List<String> providers = this.migrationService.availableProviders();
            if (!providers.isEmpty()) {
                sender.sendMessage("Available providers: " + String.join(", ", providers));
            }
        }
        return true;
    }

    private BigDecimal parseAmount(final CommandSender sender, final String raw) {
        try {
            final BigDecimal amount = new BigDecimal(raw);
            if (amount.signum() < 0) {
                sender.sendMessage("Amount must not be negative.");
                return null;
            }
            return amount;
        } catch (final NumberFormatException exception) {
            sender.sendMessage("Invalid amount: " + raw);
            return null;
        }
    }

    private String displayName(final OfflinePlayer player) {
        return player.getName() != null ? player.getName() : player.getUniqueId().toString();
    }

    private void sendUsage(final CommandSender sender) {
        sender.sendMessage("Coffers commands:");
        sender.sendMessage("/coffers balance [player] [currency]");
        sender.sendMessage("/coffers pay <player> <amount> [currency]");
        sender.sendMessage("/coffers set <player> <amount> [currency]");
        sender.sendMessage("/coffers history [player] [limit]");
        sender.sendMessage("/coffers currencies");
        sender.sendMessage("/coffers migratevault [provider]");
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
        final List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("balance");
            completions.add("pay");
            completions.add("history");
            completions.add("currencies");
            if (sender.hasPermission("coffers.command.set")) {
                completions.add("set");
            }
            if (sender.hasPermission("coffers.command.migratevault")) {
                completions.add("migratevault");
            }
            return filter(completions, args[0]);
        }

        if (args.length == 2 && List.of("balance", "pay", "set", "history").contains(args[0].toLowerCase(Locale.ROOT))) {
            for (final Player online : this.plugin.getServer().getOnlinePlayers()) {
                completions.add(online.getName());
            }
            return filter(completions, args[1]);
        }

        if ((args.length == 3 && "balance".equalsIgnoreCase(args[0]))
                || (args.length == 4 && List.of("pay", "set").contains(args[0].toLowerCase(Locale.ROOT)))) {
            return filter(currencyIds(), args[args.length - 1]);
        }

        if (args.length == 2 && "migratevault".equalsIgnoreCase(args[0])) {
            return filter(this.migrationService.availableProviders(), args[1]);
        }

        return List.of();
    }

    private List<String> currencyIds() {
        return this.economy.currencies().stream().map(currency -> currency.id()).toList();
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
        final String action = switch (entry.kind()) {
            case DEPOSIT -> "deposit";
            case WITHDRAWAL -> "withdrawal";
            case TRANSFER_IN -> "transfer in";
            case TRANSFER_OUT -> "transfer out";
            case SET -> "set balance";
        };
        final StringBuilder builder = new StringBuilder();
        builder.append(action)
                .append(" ")
                .append(this.economy.format(entry.currencyId(), entry.amount()))
                .append(" | balance ")
                .append(this.economy.format(entry.currencyId(), entry.resultingBalance()));

        if (entry.counterpartyAccountId() != null && (entry.kind() == TransactionKind.TRANSFER_IN || entry.kind() == TransactionKind.TRANSFER_OUT)) {
            builder.append(" | other account ").append(entry.counterpartyAccountId());
        }
        if (entry.reason() != null && !entry.reason().isBlank()) {
            builder.append(" | ").append(entry.reason());
        }
        if (entry.actor() != null && entry.actor().actorName() != null) {
            builder.append(" | by ").append(entry.actor().actorName());
        }
        return builder.toString();
    }
}
