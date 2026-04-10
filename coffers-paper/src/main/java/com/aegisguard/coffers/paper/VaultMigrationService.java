package com.aegisguard.coffers.paper;

import com.aegisguard.coffers.api.CoffersEconomy;
import com.aegisguard.coffers.api.TransactionActor;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

final class VaultMigrationService implements MigrationGateway {

    private final JavaPlugin plugin;
    private final CoffersEconomy economy;

    VaultMigrationService(final JavaPlugin plugin, final CoffersEconomy economy) {
        this.plugin = plugin;
        this.economy = economy;
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public MigrationReport migrate(final String requestedProviderName) {
        final RegisteredServiceProvider<Economy> registration = chooseProvider(requestedProviderName);
        if (registration == null) {
            throw new IllegalStateException("No external Vault economy provider was found to migrate from.");
        }

        final Economy provider = registration.getProvider();
        final String providerName = registration.getPlugin().getName() + " / " + provider.getName();
        final String currencyId = this.economy.defaultCurrencyId();
        final TransactionActor actor = TransactionActor.migration("vault:" + registration.getPlugin().getName());

        int importedAccounts = 0;
        int updatedAccounts = 0;
        int skippedAccounts = 0;

        for (final OfflinePlayer player : Bukkit.getOfflinePlayers()) {
            final boolean hasAccount = provider.hasAccount(player);
            final double balanceValue = provider.getBalance(player);
            if (!hasAccount && Math.abs(balanceValue) < 0.0000001D) {
                skippedAccounts++;
                continue;
            }

            importedAccounts++;
            final BigDecimal targetBalance = BigDecimal.valueOf(balanceValue);
            final BigDecimal currentBalance = this.economy.getBalance(player.getUniqueId(), currencyId);
            if (currentBalance.compareTo(targetBalance) == 0) {
                skippedAccounts++;
                continue;
            }

            this.economy.setBalance(
                    player.getUniqueId(),
                    currencyId,
                    targetBalance,
                    actor,
                    "Migrated from Vault provider " + providerName
            );
            updatedAccounts++;
        }

        return new MigrationReport(providerName, importedAccounts, updatedAccounts, skippedAccounts);
    }

    @Override
    public List<String> availableProviders() {
        final List<String> providers = new ArrayList<>();
        for (final RegisteredServiceProvider<Economy> registration : providerRegistrations()) {
            providers.add(registration.getPlugin().getName() + ":" + registration.getProvider().getName());
        }
        return providers;
    }

    private RegisteredServiceProvider<Economy> chooseProvider(final String requestedProviderName) {
        final List<RegisteredServiceProvider<Economy>> registrations = providerRegistrations();
        if (registrations.isEmpty()) {
            return null;
        }

        if (requestedProviderName == null || requestedProviderName.isBlank()) {
            return registrations.getFirst();
        }

        final String needle = requestedProviderName.toLowerCase();
        return registrations.stream()
                .filter(registration -> registration.getPlugin().getName().toLowerCase().contains(needle)
                        || registration.getProvider().getName().toLowerCase().contains(needle))
                .findFirst()
                .orElse(null);
    }

    private List<RegisteredServiceProvider<Economy>> providerRegistrations() {
        return this.plugin.getServer().getServicesManager().getRegistrations(Economy.class).stream()
                .filter(registration -> registration.getPlugin() != this.plugin)
                .filter(registration -> registration.getProvider() != null)
                .sorted(Comparator.comparingInt((RegisteredServiceProvider<Economy> registration) -> registration.getPriority().ordinal()).reversed())
                .toList();
    }
}
