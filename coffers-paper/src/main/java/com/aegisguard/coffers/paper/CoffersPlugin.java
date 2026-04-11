package com.aegisguard.coffers.paper;

import com.aegisguard.coffers.api.CoffersEconomy;
import com.aegisguard.coffers.api.CurrencyDefinition;
import com.aegisguard.coffers.api.CurrencyFormat;
import java.math.BigDecimal;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class CoffersPlugin extends JavaPlugin {

    private CoffersEconomyService economy;
    private Object vaultEconomyProvider;
    private MigrationGateway migrationService =
            new UnavailableMigrationGateway("Vault is not installed, so Vault migration is unavailable.");

    @Override
    public void onEnable() {
        saveDefaultConfig();

        try {
            final List<CurrencyDefinition> currencies = loadCurrencies();
            final EconomyStorage storage = createStorage();
            storage.initialize();

            final String defaultCurrencyId = getConfig().getString("currencies.default",
                    currencies.isEmpty() ? "coins" : currencies.getFirst().id()
            );
            final int historyLimit = getConfig().getInt("history.max-per-account", 50);
            final StorageSnapshot snapshot = storage.load();

            this.economy = new CoffersEconomyService(
                    currencies,
                    defaultCurrencyId,
                    storage,
                    historyLimit,
                    snapshot,
                    getLogger()
            );
        } catch (final Exception exception) {
            getLogger().severe("Failed to start Coffers: " + exception.getMessage());
            exception.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getServicesManager().register(CoffersEconomy.class, this.economy, this, ServicePriority.Normal);

        final PluginCommand command = Objects.requireNonNull(getCommand("coffers"), "coffers command missing from plugin.yml");
        final CoffersCommand executor = new CoffersCommand(this, this.economy);
        command.setExecutor(executor);
        command.setTabCompleter(executor);

        registerVaultCompatibility();
        configureMigrationGateway();

        getLogger().info("Coffers is ready.");
    }

    @Override
    public void onDisable() {
        getServer().getServicesManager().unregisterAll(this);
        this.vaultEconomyProvider = null;
        this.migrationService = new UnavailableMigrationGateway("Vault is not installed, so Vault migration is unavailable.");
        if (this.economy != null) {
            this.economy.close();
        }
    }

    public CoffersEconomy economy() {
        return this.economy;
    }

    public MigrationGateway migrationService() {
        return this.migrationService;
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
        registerVaultEconomyService(this.vaultEconomyProvider);
        getLogger().info("Registered Coffers as a Vault economy provider.");
    }

    private void configureMigrationGateway() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            this.migrationService = new UnavailableMigrationGateway("Vault is not installed, so Vault migration is unavailable.");
            return;
        }
        this.migrationService = new VaultMigrationService(this, this.economy);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void registerVaultEconomyService(final Object provider) {
        try {
            final Class economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            getServer().getServicesManager().register(economyClass, provider, this, ServicePriority.High);
        } catch (final ClassNotFoundException exception) {
            throw new IllegalStateException("Vault classes were not available for Coffers registration.", exception);
        }
    }

    private List<CurrencyDefinition> loadCurrencies() {
        final List<CurrencyDefinition> currencies = new ArrayList<>();
        final ConfigurationSection definitionsSection = getConfig().getConfigurationSection("currencies.definitions");
        if (definitionsSection != null) {
            for (final String currencyId : definitionsSection.getKeys(false)) {
                final ConfigurationSection section = definitionsSection.getConfigurationSection(currencyId);
                if (section == null) {
                    continue;
                }
                currencies.add(readCurrency(currencyId, section));
            }
        }

        if (!currencies.isEmpty()) {
            return currencies;
        }

        final CurrencyFormat fallbackFormat = new CurrencyFormat(true, false, true, true, true);
        currencies.add(new CurrencyDefinition(
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

    private CurrencyDefinition readCurrency(final String currencyId, final ConfigurationSection section) {
        final ConfigurationSection formatSection = section.getConfigurationSection("format");
        final CurrencyFormat format = new CurrencyFormat(
                formatSection == null || formatSection.getBoolean("symbol-first", true),
                formatSection != null && formatSection.getBoolean("space-between-symbol-and-amount", false),
                formatSection == null || formatSection.getBoolean("space-between-amount-and-name", true),
                formatSection == null || formatSection.getBoolean("use-grouping", true),
                formatSection == null || formatSection.getBoolean("show-trailing-zeros", true)
        );

        return new CurrencyDefinition(
                currencyId.toLowerCase(),
                section.getString("singular", currencyId),
                section.getString("plural", currencyId + "s"),
                section.getString("symbol", ""),
                section.getInt("fractional-digits", 2),
                new BigDecimal(section.getString("starting-balance", "0")),
                format
        );
    }

    private EconomyStorage createStorage() {
        final StorageType storageType = StorageType.fromConfig(getConfig().getString("storage.type", "yaml"));
        return switch (storageType) {
            case YAML -> new YamlEconomyStorage(
                    this,
                    getConfig().getString("storage.yaml.accounts-file", "accounts.yml"),
                    getConfig().getString("storage.yaml.history-file", "history.yml")
            );
            case SQLITE -> {
                final String databaseFile = getConfig().getString("storage.sqlite.file", "coffers.db");
                final File resolvedFile = new File(getDataFolder(), databaseFile);
                yield new SqlEconomyStorage("jdbc:sqlite:" + resolvedFile.getAbsolutePath(), new Properties());
            }
            case MYSQL -> {
                final String host = getConfig().getString("storage.mysql.host", "localhost");
                final int port = getConfig().getInt("storage.mysql.port", 3306);
                final String database = getConfig().getString("storage.mysql.database", "coffers");
                final String parameters = getConfig().getString(
                        "storage.mysql.parameters",
                        "useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
                );
                final Properties properties = new Properties();
                properties.setProperty("user", getConfig().getString("storage.mysql.username", "root"));
                properties.setProperty("password", getConfig().getString("storage.mysql.password", ""));
                yield new SqlEconomyStorage(
                        "jdbc:mysql://" + host + ":" + port + "/" + database + "?" + parameters,
                        properties
                );
            }
        };
    }
}
