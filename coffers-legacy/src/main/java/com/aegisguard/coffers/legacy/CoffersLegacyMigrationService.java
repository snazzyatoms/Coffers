package com.aegisguard.coffers.legacy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

final class CoffersLegacyMigrationService implements LegacyMigrationGateway {

    private final JavaPlugin plugin;
    private final CoffersLegacyEconomyService economy;

    CoffersLegacyMigrationService(final JavaPlugin plugin, final CoffersLegacyEconomyService economy) {
        this.plugin = plugin;
        this.economy = economy;
    }

    public boolean available() {
        return true;
    }

    public LegacyMigrationReport migrate(final String requestedProviderName) {
        RegisteredServiceProvider<Economy> registration = chooseProvider(requestedProviderName);
        if (registration == null) {
            throw new IllegalStateException("No external Vault economy provider was found to migrate from.");
        }

        Economy provider = registration.getProvider();
        String providerName = registration.getPlugin().getName() + " / " + provider.getName();
        String currencyId = this.economy.getDefaultCurrencyId();
        LegacyTransactionActor actor = LegacyTransactionActor.migration("vault:" + registration.getPlugin().getName());

        int importedAccounts = 0;
        int updatedAccounts = 0;
        int skippedAccounts = 0;

        OfflinePlayer[] players = Bukkit.getOfflinePlayers();
        for (OfflinePlayer player : players) {
            boolean hasAccount = provider.hasAccount(player);
            double balanceValue = provider.getBalance(player);
            if (!hasAccount && Math.abs(balanceValue) < 0.0000001D) {
                skippedAccounts++;
                continue;
            }

            importedAccounts++;
            BigDecimal targetBalance = BigDecimal.valueOf(balanceValue);
            BigDecimal currentBalance = this.economy.getBalance(player.getUniqueId(), currencyId);
            if (currentBalance.compareTo(targetBalance) == 0) {
                skippedAccounts++;
                continue;
            }

            this.economy.setBalance(player.getUniqueId(), currencyId, targetBalance, actor, "Migrated from Vault provider " + providerName);
            updatedAccounts++;
        }

        return new LegacyMigrationReport(providerName, importedAccounts, updatedAccounts, skippedAccounts);
    }

    public List<String> availableProviders() {
        List<String> providers = new ArrayList<String>();
        List<RegisteredServiceProvider<Economy>> registrations = providerRegistrations();
        for (RegisteredServiceProvider<Economy> registration : registrations) {
            providers.add(registration.getPlugin().getName() + ":" + registration.getProvider().getName());
        }
        return providers;
    }

    private RegisteredServiceProvider<Economy> chooseProvider(final String requestedProviderName) {
        List<RegisteredServiceProvider<Economy>> registrations = providerRegistrations();
        if (registrations.isEmpty()) {
            return null;
        }

        if (requestedProviderName == null || requestedProviderName.trim().isEmpty()) {
            return registrations.get(0);
        }

        String needle = requestedProviderName.toLowerCase();
        for (RegisteredServiceProvider<Economy> registration : registrations) {
            if (registration.getPlugin().getName().toLowerCase().contains(needle)
                    || registration.getProvider().getName().toLowerCase().contains(needle)) {
                return registration;
            }
        }
        return null;
    }

    private List<RegisteredServiceProvider<Economy>> providerRegistrations() {
        List<RegisteredServiceProvider<Economy>> registrations = new ArrayList<RegisteredServiceProvider<Economy>>(
                this.plugin.getServer().getServicesManager().getRegistrations(Economy.class)
        );
        List<RegisteredServiceProvider<Economy>> filtered = new ArrayList<RegisteredServiceProvider<Economy>>();
        for (RegisteredServiceProvider<Economy> registration : registrations) {
            if (registration.getPlugin() != this.plugin && registration.getProvider() != null) {
                filtered.add(registration);
            }
        }
        Collections.sort(filtered, new Comparator<RegisteredServiceProvider<Economy>>() {
            public int compare(final RegisteredServiceProvider<Economy> left, final RegisteredServiceProvider<Economy> right) {
                return Integer.compare(right.getPriority().ordinal(), left.getPriority().ordinal());
            }
        });
        return filtered;
    }
}
