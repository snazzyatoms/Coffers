# Configuration

This page explains the main configuration options for Coffers and Coffers Legacy.

Both plugin lines use a `config.yml` file, and both support the same major configuration areas:

- Vault compatibility
- chat styling
- payment preference storage
- bank registry settings
- automated safety backups
- storage backend selection
- transaction history limits
- currency definitions
- currency formatting rules

## Where the Config Comes From

The default configuration templates are included with each plugin line:

- modern line defaults: `coffers-paper/src/main/resources/config.yml`
- legacy line defaults: `coffers-legacy/src/main/resources/config.yml`

After first startup, the plugin will generate its configuration files inside its plugin data folder on the server.

## Main Config Sections

The main sections are:

- `compatibility`
- `chat-style`
- `runtime`
- `payments`
- `banks`
- `safety`
- `storage`
- `history`
- `currencies`

## Vault Compatibility

The Vault bridge setting is controlled here:

```yml
compatibility:
  vault-bridge: auto
```

Available modes:

- `auto`
  register with Vault only when Vault is installed
- `enabled`
  always expose the Vault bridge when Vault is present
- `disabled`
  never register the Vault bridge

Recommended usage:

- use `auto` for most servers
- use `enabled` only if you specifically want Coffers to register whenever Vault is available
- use `disabled` if you do not want Vault integration at all

## Chat Style

Coffers includes a configurable message palette so server owners can change the look of command output without editing plugin code.

```yml
chat-style:
  prefix: "&6[Coffers]&r "
  primary: "&f"
  secondary: "&7"
  accent: "&6"
  success: "&a"
  error: "&c"
  warning: "&e"
  highlight: "&b"
  usage: "&e"
  bullet: "&8- &r"
```

This controls:

- the prefix shown before Coffers messages
- the main color roles used in replies
- the styling of usage output and bullet lists

## Payment Preference Storage

Player `/paytoggle` state is stored in its own file:

```yml
payments:
  preferences-file: payment-preferences.yml
```

Legacy uses its own filename by default, but the purpose is the same.

## Bank Registry

Coffers stores bank names separately from player account balances:

```yml
banks:
  registry-file: banks.yml
  auto-create-player-bank: true
  player-bank-suffix: "-bank"
```

Key options:

- `registry-file`
  stores bank registry metadata
- `auto-create-player-bank`
  creates a personal bank entry for a player when they join
- `player-bank-suffix`
  controls how automatically created player bank names are built

Player banks are optional. A player's normal economy account still remains their main live balance.

## Safety And Recovery

The `safety` section controls Coffers' automatic recovery helpers:

```yml
safety:
  auto-backups:
    enabled: true
    interval-minutes: 30
    retention: 12
  startup-safety-copy: true
  shutdown-safety-copy: true
```

### Automatic backups

- `enabled`
  turns scheduled safety backups on or off
- `interval-minutes`
  controls how often automatic backups run
- `retention`
  controls how many automated safety backups are kept

Common interval values:

- `15`
- `30`
- `60`

Any positive minute interval can be used.

### Startup and shutdown safety copies

- `startup-safety-copy`
  creates a recovery snapshot when Coffers starts
- `shutdown-safety-copy`
  creates a recovery snapshot when Coffers stops

These settings help protect against accidental overwrite, migration mistakes, or unexpected runtime problems.

## Storage Configuration

The active storage backend is selected here:

```yml
storage:
  type: yaml
```

Supported values:

- `yaml`
- `sqlite`
- `mysql`

### YAML storage

YAML is the simplest option and is enabled by default.

Modern line example:

```yml
storage:
  type: yaml
  yaml:
    accounts-file: accounts.yml
    history-file: history.yml
```

Legacy line example:

```yml
storage:
  type: yaml
  yaml:
    accounts-file: legacy-accounts.yml
    history-file: legacy-history.yml
```

Use YAML when:

- your server is small
- you want the simplest possible setup
- you do not need a SQL database

### SQLite storage

SQLite stores data in a local database file on the server.

Use SQLite when:

- you want structured persistence
- your economy lives on one server
- you do not want to manage MySQL

### MySQL storage

MySQL is intended for larger setups or environments that need a shared database.

Use MySQL when:

- you run a larger server environment
- you want central database storage
- you are comfortable managing SQL credentials and database access

## History Configuration

Transaction history size is controlled here:

```yml
history:
  max-per-account: 50
```

This value controls how many recent ledger entries are kept per account in storage.

You may want:

- a lower value for smaller or lighter setups
- a higher value if you want more audit history for rollback and recovery

## Currency Configuration

Currencies are defined in the `currencies` section.

Example:

```yml
currencies:
  default: coins
  definitions:
    coins:
      enabled: true
      singular: coin
      plural: coins
      symbol: "$"
      fractional-digits: 2
      starting-balance: 100.00
```

Each currency can define:

- whether it is enabled
- singular name
- plural name
- symbol
- fractional digits
- starting balance
- display formatting rules

## Default Currency

The default currency is set here:

```yml
currencies:
  default: coins
```

Important:

- the default currency must exist in `currencies.definitions`
- Coffers validates this at startup
- if the configured default currency is missing, startup will fail instead of silently using a broken configuration

## Formatting Rules

Each currency can define its own display rules:

```yml
format:
  symbol-first: true
  space-between-symbol-and-amount: false
  space-between-amount-and-name: true
  use-grouping: true
  show-trailing-zeros: true
```

These rules control how formatted balance output appears in commands, PlaceholderAPI output, and plugin-facing displays.

## Recommended Starting Configs

### Small server

- storage type: `yaml`
- history limit: `25` to `50`
- Vault mode: `auto`
- automatic backups: `enabled` at `30` or `60` minutes

### Single-server production setup

- storage type: `sqlite`
- history limit: `50` or higher
- Vault mode: `auto`
- automatic backups: `enabled` at `15` or `30` minutes

### Larger or shared deployment

- storage type: `mysql`
- history limit: based on your storage and audit needs
- Vault mode: `auto` or `enabled`, depending on your plugin environment
- automatic backups: enabled with retention sized for your maintenance workflow

## Common Configuration Mistakes

Avoid these common issues:

- setting a default currency that is not defined
- enabling MySQL without valid credentials
- choosing the wrong storage type for your server setup
- forgetting to review the `safety` section after first install
- assuming modern and legacy lines use identical storage filenames

## Next Step

After configuration, continue to:

- [Commands](Commands)
- [Storage Backends](Storage-Backends)
- [Vault Compatibility](Vault-Compatibility)
- [Migration Guide](Migration-Guide)
