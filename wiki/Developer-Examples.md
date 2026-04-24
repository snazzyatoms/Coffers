# Developer Examples

This page gives a few practical examples for plugin authors who want to integrate with Coffers directly.

For the richer developer surface in `v0.2`, prefer the Coffers API or SPI instead of treating Coffers only as a Vault provider.

## Getting the Modern API Service

Use the Bukkit services manager to request:

- `CoffersEconomy`

Typical flow:

1. request the service during plugin enable
2. store the returned economy reference
3. check for null and handle Coffers not being present
4. use Coffers methods directly instead of generic Vault calls

## Reading a Balance

Typical read flow:

1. obtain the target player's UUID
2. call `getBalance(uuid, currencyId)` or `getBalance(uuid)`
3. use `format(...)` if you want Coffers-native display formatting

Recommended:

- use `format(...)` for chat or GUI presentation
- avoid hardcoding symbols or decimal rules

## Depositing Funds

Typical deposit flow:

1. create a `TransactionActor` describing your plugin
2. call `deposit(...)`
3. inspect the returned `TransactionResult`

Recommended:

- always check `successful()`
- inspect `failure()` and `message()` for clear handling
- do not assume every balance change succeeds

## Multi-Currency Plugins

If your plugin supports specific currencies:

1. call `currency(currencyId)`
2. verify the currency exists
3. use the returned `CurrencyDefinition` for naming, symbols, and formatting rules

This keeps your plugin aligned with the server's configured Coffers economy instead of hardcoding one display style.

## History and Auditing

Coffers includes ledger-aware transaction tracking.

Useful API concepts:

- `TransactionResult`
- `LedgerEntry`
- `TransactionActor`
- `TransactionKind`

In `v0.2`, ledger entries also carry:

- previous balance
- resulting balance
- reversal reference information for rollback-aware audit trails

That makes Coffers a better fit for admin tools, transaction inspection, rollback utilities, and detailed economy integrations.

## Native SPI vs Vault

For lightweight direct integration, use the Coffers SPI service where appropriate.

Good use cases:

- detect whether Coffers is installed
- use Coffers directly without requiring Vault
- prefer Coffers first and fall back to Vault only when needed

Recommended strategy:

1. look for Coffers-native services first
2. use Vault only as a fallback for older environments

## Bank-Aware Integrations

Starting in `v0.2`, Coffers includes built-in bank support for server-side administration workflows.

If your plugin needs to interact with bank-style economy containers:

- treat banks as managed Coffers-side accounts
- keep your own plugin logic clear about whether an operation targets a player or a bank
- use Coffers transaction results and history where possible for consistency

## Placeholder-Friendly Design

If your plugin renders economy information to placeholders, scoreboards, or GUI text:

- prefer Coffers formatting rules for final display
- use raw balance values only when you specifically need machine-readable numbers

## Summary

For Coffers-aware plugins in `v0.2`, the best long-term direction is:

- direct Coffers API for rich integrations
- native Coffers SPI for lightweight detection and hookup
- Vault only as a compatibility fallback

## Next Step

Continue to:

- [Developer-API](Developer-API)
- [Commands](Commands)
- [Standalone-First](Standalone-First)
- [Home](Home)
