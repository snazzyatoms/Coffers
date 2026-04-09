package com.aegisguard.coffers.paper;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class CoffersPlaceholderExpansion extends PlaceholderExpansion {

    private final CoffersPlugin plugin;

    CoffersPlaceholderExpansion(final CoffersPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "coffers";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Aegis Guard";
    }

    @Override
    public @NotNull String getVersion() {
        return this.plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(final OfflinePlayer player, final @NotNull String params) {
        if (this.plugin.economyService() == null) {
            return "";
        }

        if ("currency_default".equalsIgnoreCase(params)) {
            return this.plugin.economy().defaultCurrencyId();
        }

        if ("balance".equalsIgnoreCase(params)) {
            if (player == null) {
                return "";
            }
            return this.plugin.economy().format(this.plugin.economy().getBalance(player.getUniqueId()));
        }

        if (params.toLowerCase().startsWith("balance_")) {
            if (player == null) {
                return "";
            }
            final String currencyId = params.substring("balance_".length());
            if (this.plugin.economy().currency(currencyId).isEmpty()) {
                return "";
            }
            return this.plugin.economy().format(currencyId, this.plugin.economy().getBalance(player.getUniqueId(), currencyId));
        }

        final TopPlaceholderQuery topNameQuery = parseTopPlaceholderQuery(params, "top_name_", this.plugin.economy().defaultCurrencyId());
        if (topNameQuery != null) {
            return topName(topNameQuery.currencyId(), topNameQuery.rankToken());
        }

        final TopPlaceholderQuery topBalanceQuery = parseTopPlaceholderQuery(params, "top_balance_", this.plugin.economy().defaultCurrencyId());
        if (topBalanceQuery != null) {
            return topBalance(topBalanceQuery.currencyId(), topBalanceQuery.rankToken());
        }

        return "";
    }

    static TopPlaceholderQuery parseTopPlaceholderQuery(
            final String params,
            final String prefix,
            final String defaultCurrencyId
    ) {
        if (!params.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return null;
        }

        final String suffix = params.substring(prefix.length());
        if (suffix.isBlank()) {
            return null;
        }

        final int lastUnderscore = suffix.lastIndexOf('_');
        if (lastUnderscore < 0) {
            return parseRank(suffix) < 0 ? null : new TopPlaceholderQuery(defaultCurrencyId, suffix);
        }

        final String currencyId = suffix.substring(0, lastUnderscore);
        final String rankToken = suffix.substring(lastUnderscore + 1);
        if (currencyId.isBlank() || parseRank(rankToken) < 0) {
            return null;
        }
        return new TopPlaceholderQuery(currencyId, rankToken);
    }

    private String topName(final String currencyId, final String rankToken) {
        final int index = parseRank(rankToken);
        if (index < 0 || this.plugin.economy().currency(currencyId).isEmpty()) {
            return "";
        }

        final var topAccounts = this.plugin.economy().topAccounts(currencyId, index + 1);
        if (topAccounts.size() <= index) {
            return "";
        }

        final OfflinePlayer player = Bukkit.getOfflinePlayer(topAccounts.get(index).accountId());
        return player.getName() == null ? topAccounts.get(index).accountId().toString() : player.getName();
    }

    private String topBalance(final String currencyId, final String rankToken) {
        final int index = parseRank(rankToken);
        if (index < 0 || this.plugin.economy().currency(currencyId).isEmpty()) {
            return "";
        }

        final var topAccounts = this.plugin.economy().topAccounts(currencyId, index + 1);
        if (topAccounts.size() <= index) {
            return "";
        }

        return this.plugin.economy().format(currencyId, topAccounts.get(index).balance());
    }

    private static int parseRank(final String rankToken) {
        try {
            return Math.max(0, Integer.parseInt(rankToken) - 1);
        } catch (final NumberFormatException exception) {
            return -1;
        }
    }

    record TopPlaceholderQuery(String currencyId, String rankToken) {
    }
}
