package com.aegisguard.coffers.paper;

import com.aegisguard.coffers.api.CoffersEconomy;
import com.aegisguard.coffers.api.TransactionResult;
import java.math.BigDecimal;
import java.util.ArrayList;
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

    CoffersCommand(final CoffersPlugin plugin, final CoffersEconomy economy) {
        this.plugin = plugin;
        this.economy = economy;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "balance" -> handleBalance(sender, args);
            case "pay" -> handlePay(sender, args);
            case "set" -> handleSet(sender, args);
            default -> {
                sendUsage(sender);
                yield true;
            }
        };
    }

    private boolean handleBalance(final CommandSender sender, final String[] args) {
        if (args.length == 1) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Console must specify a player.");
                return true;
            }

            final BigDecimal balance = this.economy.getBalance(player.getUniqueId());
            sender.sendMessage("Balance: " + this.economy.format(balance));
            return true;
        }

        if (!sender.hasPermission("coffers.command.balance.others")) {
            sender.sendMessage("You do not have permission to view another player's balance.");
            return true;
        }

        final OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        final BigDecimal balance = this.economy.getBalance(target.getUniqueId());
        sender.sendMessage(displayName(target) + " has " + this.economy.format(balance));
        return true;
    }

    private boolean handlePay(final CommandSender sender, final String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use /coffers pay.");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage("Usage: /coffers pay <player> <amount>");
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

        final TransactionResult result = this.economy.transfer(
                player.getUniqueId(),
                target.getUniqueId(),
                amount,
                "Player transfer"
        );

        if (!result.successful()) {
            sender.sendMessage("Payment failed: " + result.message());
            return true;
        }

        sender.sendMessage("Sent " + this.economy.format(result.amount()) + " to " + displayName(target) + ".");
        return true;
    }

    private boolean handleSet(final CommandSender sender, final String[] args) {
        if (!sender.hasPermission("coffers.command.set")) {
            sender.sendMessage("You do not have permission to set balances.");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage("Usage: /coffers set <player> <amount>");
            return true;
        }

        final OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        final BigDecimal amount = parseAmount(sender, args[2]);
        if (amount == null) {
            return true;
        }

        final TransactionResult result = this.economy.setBalance(target.getUniqueId(), amount, "Admin set balance");
        sender.sendMessage("Set " + displayName(target) + " to " + this.economy.format(result.balance()) + ".");
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
        sender.sendMessage("/coffers balance [player]");
        sender.sendMessage("/coffers pay <player> <amount>");
        sender.sendMessage("/coffers set <player> <amount>");
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
        final List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("balance");
            completions.add("pay");
            if (sender.hasPermission("coffers.command.set")) {
                completions.add("set");
            }
            return filter(completions, args[0]);
        }

        if (args.length == 2 && List.of("balance", "pay", "set").contains(args[0].toLowerCase(Locale.ROOT))) {
            for (final Player online : this.plugin.getServer().getOnlinePlayers()) {
                completions.add(online.getName());
            }
            return filter(completions, args[1]);
        }

        return List.of();
    }

    private List<String> filter(final List<String> candidates, final String token) {
        final String needle = token.toLowerCase(Locale.ROOT);
        return candidates.stream()
                .filter(candidate -> candidate.toLowerCase(Locale.ROOT).startsWith(needle))
                .toList();
    }
}
