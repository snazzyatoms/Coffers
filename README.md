![Coffers Banner](assets/coffers-banner.png)

# Coffers

Coffers is a fresh, from-scratch economy platform for modern Minecraft servers.

The direction is simple:

- Keep the good part of Vault: a stable abstraction other plugins can depend on.
- Avoid the old Vault trap: baking every integration directly into one giant legacy plugin model.
- Leave room for newer ideas like richer transactions, multi-currency, async storage, and cleaner bridges.

## What Server Owners Install

Server owners should choose the plugin line that matches their server:

- `release/Coffers.jar`
- `release/Coffers-Legacy.jar`

Use:

- `Coffers.jar` for the modern line
- `Coffers-Legacy.jar` for older legacy-oriented server setups

Vault compatibility is built into the main plugin and can be set to:

- `auto`: register with Vault only when Vault is installed
- `enabled`: force Coffers to expose a Vault economy provider when Vault is present
- `disabled`: never register the Vault bridge

## Storage Backends

The modern Coffers line supports multiple persistence modes:

- `yaml`: simple local file storage for smaller servers that do not want a database
- `sqlite`: file-based SQL storage for a single server that still wants structured persistence
- `mysql`: shared database storage for larger or networked setups

Storage mode is configured in `coffers-paper/src/main/resources/config.yml`.

The legacy line also ships with YAML, SQLite, and MySQL options through its own legacy configuration.

## Project Layout

- `coffers-paper`: the main Paper plugin, including commands, storage wiring, Vault compatibility, and migration support
- `coffers-api`: the shared developer API for currencies, ledger entries, transaction results, and richer integrations
- `coffers-legacy`: the legacy-compatible plugin line for older server baselines
- storage backends: YAML for simple setups, SQLite for single-server persistence, and MySQL for shared database deployments
- built-in compatibility: Coffers can register as a Vault economy provider without needing a separate bridge plugin

## Early Goals

- Provide a clean economy service for Aegis Guard and future plugins.
- Keep Vault support as a built-in bridge, not the center of the design.
- Start with a small, understandable baseline before adding persistence and advanced features.
- Make server-owner setup simple while still giving developers a real API to build against.

## Current State

Right now Coffers includes:

- a Paper plugin bootstrap
- a separate legacy plugin line for older server baselines
- persistent YAML, SQLite, and MySQL storage options
- configurable currencies with symbols, starting balances, fractional digits, and formatting rules
- transaction history with audit metadata
- built-in Vault compatibility
- migration helpers for existing Vault-based economy setups
- a richer API for plugin-to-plugin integrations
- starter commands:
  - `/coffers balance [player]`
  - `/coffers pay <player> <amount>`
  - `/coffers set <player> <amount>`
  - `/coffers history [player] [limit]`
  - `/coffers currencies`
  - `/coffers migratevault [provider]`

Current compatibility settings live in `coffers-paper/src/main/resources/config.yml`.

## Modern And Legacy Lines

Coffers now ships in two server-owner lines:

- `Coffers.jar`
  - intended for the modern line
  - built for the current Paper-focused codebase

- `Coffers-Legacy.jar`
  - intended for older Spigot/Paper/Purpur-style server setups
  - compiled against an older server/API baseline so legacy servers have their own supported line

This approach is cleaner than pretending one jar can safely support every Minecraft server generation at once.

## Transaction History And Audit Metadata

Every balance-changing operation recorded by Coffers can include:

- transaction kind
- currency
- amount
- resulting balance
- reason
- actor type
- actor identity or source
- timestamp
- transfer reference identifiers for related entries

This gives plugin authors and server admins a stronger base for auditing than a simple balance-only economy model.

## Currency And Formatting Support

Coffers supports multiple currencies with per-currency rules, including:

- singular and plural display names
- symbols
- starting balances
- fractional digits
- grouping separators
- symbol placement
- spacing rules
- trailing zero display rules

The default configuration ships with `coins` and `gems` as examples.

## Migration Helpers

Coffers can import balances from another Vault-backed economy provider already present on the server.

Use:

- `/coffers migratevault`
- `/coffers migratevault <provider>`

This is intended to make adoption easier for servers currently running a different economy plugin behind Vault.

## Developer API

The API jar exposes richer integration types than the original baseline prototype, including:

- currency definitions and format rules
- account snapshots
- transaction results
- ledger entries
- transaction actor metadata
- transaction kinds

This gives other plugins a better foundation than relying only on legacy Vault-style balance calls.

## Downloads

- Server owners: use `release/Coffers.jar`
- Older legacy servers: use `release/Coffers-Legacy.jar`
- Developers: optional `release/Coffers-API.jar`

## What Informed the Design

These ideas were borrowed conceptually, not by copying code:

- Vault: a central abstraction that many plugins know how to consume
- Treasury and other modern economy APIs: better separation between API and implementation concerns
- newer Vault refresh efforts: focusing on modern servers without dragging every old integration into the core design

## License

The current recommendation for this project is `Apache-2.0`.
