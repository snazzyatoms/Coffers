package com.aegisguard.coffers.paper;

import com.aegisguard.coffers.api.CoffersEconomy;
import java.math.BigDecimal;
import java.util.Objects;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class CoffersPlugin extends JavaPlugin {

    private InMemoryCoffersEconomy economy;
    private Economy vaultEconomyProvider;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.economy = new InMemoryCoffersEconomy(
                getConfig().getString("currency.singular", "coin"),
                getConfig().getString("currency.plural", "coins"),
                getConfig().getString("currency.symbol", "$"),
                BigDecimal.valueOf(getConfig().getDouble("economy.starting-balance", 0.0D)),
                getConfig().getInt("economy.fractional-digits", 2)
        );

        getServer().getServicesManager().register(CoffersEconomy.class, this.economy, this, ServicePriority.Normal);

        final PluginCommand command = Objects.requireNonNull(getCommand("coffers"), "coffers command missing from plugin.yml");
        final CoffersCommand executor = new CoffersCommand(this, this.economy);
        command.setExecutor(executor);
        command.setTabCompleter(executor);

        registerVaultCompatibility();

        getLogger().info("Coffers is ready.");
    }

    @Override
    public void onDisable() {
        getServer().getServicesManager().unregisterAll(this);
    }

    public CoffersEconomy economy() {
        return this.economy;
    }

    private void registerVaultCompatibility() {
        final VaultBridgeMode bridgeMode = VaultBridgeMode.fromConfig(getConfig().getString("compatibility.vault-bridge", "auto"));
        final boolean vaultPresent = getServer().getPluginManager().getPlugin("Vault") != null;

        if (bridgeMode == VaultBridgeMode.DISABLED) {
            getLogger().info("Vault compatibility is disabled in config.");
            return;
        }

        if (!vaultPresent) {
            if (bridgeMode == VaultBridgeMode.ENABLED) {
                getLogger().warning("Vault compatibility was forced on, but Vault is not installed.");
            } else {
                getLogger().info("Vault not detected. Skipping Vault compatibility bridge.");
            }
            return;
        }

        this.vaultEconomyProvider = new CoffersVaultEconomy(this, this.economy);
        getServer().getServicesManager().register(Economy.class, this.vaultEconomyProvider, this, ServicePriority.High);
        getLogger().info("Registered Coffers as a Vault economy provider.");
    }
}
