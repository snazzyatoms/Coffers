package com.aegisguard.coffers.legacy;

import com.aegisguard.coffers.spi.NativeCoffersEconomy;
import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class CoffersLegacyPlugin extends JavaPlugin {

    private CoffersLegacyEconomyService economy;
    private Object vaultEconomyProvider;
    private LegacyMigrationGateway migrationService =
            new UnavailableLegacyMigrationGateway("Vault is not installed, so Vault migration is unavailable.");

    public void onEnable() {
        saveDefaultConfig();

        try {
            List<LegacyCurrencyDefinition> currencies = loadCurrencies();
            LegacyEconomyStorage storage = createStorage();
            storage.initialize();

            String defaultCurrencyId = getConfig().getString("currencies.default", currencies.isEmpty() ? "coins" : currencies.get(0).getId());
            int historyLimit = getConfig().getInt("history.max-per-account", 50);
            LegacyStorageSnapshot snapshot = storage.load();

            this.economy = new CoffersLegacyEconomyService(currencies, defaultCurrencyId, storage, historyLimit, snapshot, getLogger());
        } catch (Exception exception) {
            getLogger().severe("Failed to start Coffers Legacy: " + exception.getMessage());
            exception.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        PluginCommand command = Objects.requireNonNull(getCommand("cofferslegacy"), "cofferslegacy command missing from plugin.yml");
        getServer().getServicesManager().register(NativeCoffersEconomy.class, new NativeLegacyCoffersEconomyBridge(this.economy), this, ServicePriority.High);

        CoffersLegacyCommand executor = new CoffersLegacyCommand(this, this.economy);
        command.setExecutor(executor);
        command.setTabCompleter(executor);

        registerVaultCompatibility();
        configureMigrationGateway();
        getLogger().info("Coffers Legacy is ready.");
    }

    public void onDisable() {
        getServer().getServicesManager().unregisterAll(this);
        this.vaultEconomyProvider = null;
        this.migrationService = new UnavailableLegacyMigrationGateway("Vault is not installed, so Vault migration is unavailable.");
        if (this.economy != null) {
            this.economy.close();
        }
    }

    private void registerVaultCompatibility() {
        LegacyVaultBridgeMode bridgeMode = LegacyVaultBridgeMode.fromConfig(getConfig().getString("compatibility.vault-bridge", "auto"));
        boolean vaultPresent = getServer().getPluginManager().getPlugin("Vault") != null;

        if (bridgeMode == LegacyVaultBridgeMode.DISABLED) {
            getLogger().info("Vault compatibility is disabled in config.");
            return;
        }

        if (!vaultPresent) {
            if (bridgeMode == LegacyVaultBridgeMode.ENABLED) {
                getLogger().warning("Vault compatibility was forced on, but Vault is not installed.");
            } else {
                getLogger().info("Vault not detected. Skipping Vault compatibility bridge.");
            }
            return;
        }

        this.vaultEconomyProvider = new CoffersLegacyVaultEconomy(this, this.economy);
        registerVaultEconomyService(this.vaultEconomyProvider);
        getLogger().info("Registered Coffers Legacy as a Vault economy provider.");
    }

    private void configureMigrationGateway() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            this.migrationService = new UnavailableLegacyMigrationGateway("Vault is not installed, so Vault migration is unavailable.");
            return;
        }
        this.migrationService = new CoffersLegacyMigrationService(this, this.economy);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void registerVaultEconomyService(final Object provider) {
        try {
            Class economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            getServer().getServicesManager().register(economyClass, provider, this, ServicePriority.High);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Vault classes were not available for Coffers Legacy registration.", exception);
        }
    }

    private List<LegacyCurrencyDefinition> loadCurrencies() {
        List<LegacyCurrencyDefinition> currencies = new ArrayList<LegacyCurrencyDefinition>();
        ConfigurationSection definitionsSection = getConfig().getConfigurationSection("currencies.definitions");
        if (definitionsSection != null) {
            for (String currencyId : definitionsSection.getKeys(false)) {
                ConfigurationSection section = definitionsSection.getConfigurationSection(currencyId);
                if (section != null) {
                    currencies.add(readCurrency(currencyId, section));
                }
            }
        }

        if (!currencies.isEmpty()) {
            return currencies;
        }

        LegacyCurrencyFormat fallbackFormat = new LegacyCurrencyFormat(true, false, true, true, true);
        currencies.add(new LegacyCurrencyDefinition(
                "coins",
                getConfig().getString("currency.singular", "coin"),
                getConfig().getString("currency.plural", "coins"),
                getConfig().getString("currency.symbol", "$"),
                getConfig().getInt("economy.fractional-digits", 2),
                BigDecimal.valueOf(getConfig().getDouble("economy.starting-balance", 0.0D)),
                fallbackFormat
        ));
        return currencies;
    }

    private LegacyCurrencyDefinition readCurrency(final String currencyId, final ConfigurationSection section) {
        ConfigurationSection formatSection = section.getConfigurationSection("format");
        LegacyCurrencyFormat format = new LegacyCurrencyFormat(
                formatSection == null || formatSection.getBoolean("symbol-first", true),
                formatSection != null && formatSection.getBoolean("space-between-symbol-and-amount", false),
                formatSection == null || formatSection.getBoolean("space-between-amount-and-name", true),
                formatSection == null || formatSection.getBoolean("use-grouping", true),
                formatSection == null || formatSection.getBoolean("show-trailing-zeros", true)
        );

        return new LegacyCurrencyDefinition(
                currencyId.toLowerCase(Locale.ROOT),
                section.getString("singular", currencyId),
                section.getString("plural", currencyId + "s"),
                section.getString("symbol", ""),
                section.getInt("fractional-digits", 2),
                new BigDecimal(section.getString("starting-balance", "0")),
                format
        );
    }

    private LegacyEconomyStorage createStorage() {
        LegacyStorageType storageType = LegacyStorageType.fromConfig(getConfig().getString("storage.type", "yaml"));
        if (storageType == LegacyStorageType.YAML) {
            return new LegacyYamlEconomyStorage(
                    this,
                    getConfig().getString("storage.yaml.accounts-file", "legacy-accounts.yml"),
                    getConfig().getString("storage.yaml.history-file", "legacy-history.yml")
            );
        }

        if (storageType == LegacyStorageType.SQLITE) {
            String databaseFile = getConfig().getString("storage.sqlite.file", "coffers-legacy.db");
            File resolvedFile = new File(getDataFolder(), databaseFile);
            return new LegacySqlEconomyStorage("jdbc:sqlite:" + resolvedFile.getAbsolutePath(), new Properties());
        }

        String host = getConfig().getString("storage.mysql.host", "localhost");
        int port = getConfig().getInt("storage.mysql.port", 3306);
        String database = getConfig().getString("storage.mysql.database", "coffers_legacy");
        String parameters = getConfig().getString("storage.mysql.parameters", "useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        Properties properties = new Properties();
        properties.setProperty("user", getConfig().getString("storage.mysql.username", "root"));
        properties.setProperty("password", getConfig().getString("storage.mysql.password", ""));
        return new LegacySqlEconomyStorage("jdbc:mysql://" + host + ":" + port + "/" + database + "?" + parameters, properties);
    }

    CoffersLegacyEconomyService economy() {
        return this.economy;
    }

    Collection<LegacyCurrencyDefinition> currencies() {
        return this.economy.currencies();
    }

    LegacyMigrationGateway migrationService() {
        return this.migrationService;
    }
}
