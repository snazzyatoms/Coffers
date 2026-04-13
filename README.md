![Coffers Banner](assets/coffers-banner.png)

# Coffers

Coffers is a standalone-first economy platform for Minecraft servers.

It is built for server owners who want a full economy plugin they can run on its own, while still keeping Vault available as an optional compatibility layer for older plugins that may still require it.

For plugin developers, Coffers also provides native integration paths that are richer and more modern than the older balance-only model many servers are used to.

## What Coffers Offers

- standalone economy support with no Vault requirement
- optional Vault compatibility for older plugin stacks
- YAML, SQLite, and MySQL storage backends
- configurable currencies and formatting rules
- transaction history with audit-friendly metadata
- migration helpers for existing Vault-backed setups
- direct API and SPI modules for plugin developers
- separate modern and legacy plugin lines

## Which Jar Is For What?

### For server owners

- `release/Coffers.jar`
  - the modern plugin line
  - intended for modern Paper-based server environments

- `release/Coffers-Legacy.jar`
  - the legacy plugin line
  - intended for older Bukkit-family server environments

### For plugin developers

- `release/Coffers-API.jar`
  - the main developer API
  - intended for richer direct integrations with the modern Coffers line

- `release/Coffers-SPI.jar`
  - the service-provider interface module
  - intended for shared native integration contracts and service discovery style hooks

In short:

- server owners install `Coffers.jar` or `Coffers-Legacy.jar`
- developers can additionally use `Coffers-API.jar` or `Coffers-SPI.jar` depending on their integration needs

## How Vault Fits In

Vault is optional in Coffers.

If Vault is installed and enabled in config, Coffers can register itself as a Vault economy provider so that older plugins can continue working.

If Vault is not installed, Coffers still runs normally as a standalone economy plugin.

This makes Coffers useful for both kinds of server setups:

- servers that want to keep supporting older Vault-based plugins
- servers that want to move toward direct Coffers integrations over time

Vault bridge behavior can be set to:

- `auto`
- `enabled`
- `disabled`

## Storage Backends

Coffers supports three storage modes:

- `yaml`
  - simple file storage for lightweight setups
- `sqlite`
  - local database storage for a single server
- `mysql`
  - shared database storage for larger or networked environments

Both the modern and legacy lines include these storage options.

## Modern And Legacy Plugin Lines

Coffers ships in two server-owner plugin lines so each environment has a more appropriate baseline.

### Modern line

- `Coffers.jar`
- focused on the modern codebase
- includes the current admin tooling and PlaceholderAPI support

### Legacy line

- `Coffers-Legacy.jar`
- intended for older Bukkit, CraftBukkit, Spigot, and similar legacy-oriented setups
- keeps the same overall economy direction while targeting older server environments

## Core Features

Coffers currently includes:

- balance, pay, set, history, top, and currency commands
- named backups, exports, imports, and live restore support
- pay-toggle preferences
- bank support
- rollback tools for transaction correction
- startup diagnostics and migration reporting
- configurable chat styling
- configurable multi-currency support
- transaction audit history

## Developer Integrations

For plugin authors, Coffers offers two main integration layers.

### API

The API module includes rich economy types such as:

- currency definitions
- currency formatting rules
- account snapshots
- transaction results
- ledger entries
- transaction kinds
- actor and audit metadata

### SPI

The SPI module is intended for native service-style integrations where a plugin wants to hook into Coffers through a shared contract without relying on Vault.

This helps plugins target Coffers directly while still allowing server owners to decide whether Vault should be present for compatibility with older plugins.

## Migration Support

Coffers can import balances from another Vault-backed economy provider already present on the server.

This helps server owners move to Coffers more gradually rather than rebuilding balances by hand.

Common commands include:

- `/coffers migratevault`
- `/coffers migratevault <provider>`
- `/cofferslegacy migratevault <provider>`

## Documentation

The project wiki is included in this repository:

- `wiki/Home.md`
- `wiki/Installation.md`
- `wiki/Configuration.md`
- `wiki/Storage-Backends.md`
- `wiki/Vault-Compatibility.md`
- `wiki/Developer-API.md`
- `wiki/Developer-Examples.md`

## Project Layout

- `coffers-paper`
  - modern plugin implementation
- `coffers-legacy`
  - legacy plugin implementation
- `coffers-api`
  - developer-facing API module
- `coffers-spi`
  - shared service-provider interface module

## Downloads

- modern servers: `release/Coffers.jar`
- legacy servers: `release/Coffers-Legacy.jar`
- developers: `release/Coffers-API.jar`
- native integration contracts: `release/Coffers-SPI.jar`

## License

The current recommendation for this project is `Apache-2.0`.
