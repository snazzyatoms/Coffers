# Standalone-First Direction

This page explains the direction Coffers is taking starting with `v0.2`.

## The Short Version

Coffers is being built as a standalone economy platform first.

That means:

- server owners should be able to run Coffers without Vault installed
- plugin developers should be able to integrate with Coffers directly through `Coffers-API.jar`
- Vault compatibility should exist only to help older plugin ecosystems keep working during migration

## Why This Matters

Vault became the common target for many older economy-aware plugins, but it was designed around a simpler economy model.

Coffers is intended to go further by supporting:

- richer transaction results
- transaction history and audit metadata
- configurable currencies and formatting rules
- more direct plugin-to-plugin integrations
- cleaner storage and migration workflows

Those features fit better in a Coffers-native API than in the older Vault abstraction.

## What Standalone Means In Coffers

For Coffers, standalone means:

- the main economy system does not depend on Vault to function
- Vault is not required for storage, balances, commands, currencies, or history
- the Coffers API is the preferred integration surface for new development
- Vault support is optional and controlled by config

If Vault is not present, Coffers should still operate normally as its own platform.

## What Vault Compatibility Is For

Vault compatibility still matters, but its role is narrower.

It exists to help with:

- older third-party plugins that still require Vault
- migration from older economy stacks
- mixed server environments where not every plugin has moved to Coffers yet

It should not define the future feature set of Coffers.

## The v0.2 Position

Starting in `v0.2`, the recommended way to think about the project is:

- `Coffers.jar` is the main economy platform
- `Coffers-Legacy.jar` is the older-server line
- `Coffers-API.jar` is the preferred developer integration target
- Vault support is a built-in bridge for compatibility, not the center of the architecture

## Recommended Server Strategy

For most server owners:

- install Coffers as the primary economy plugin
- only install and enable Vault support if another plugin still depends on Vault
- use Coffers-native features and configuration as the source of truth

## Recommended Developer Strategy

For new plugin development:

- prefer the Coffers API over Vault
- use Coffers transaction results and ledger data directly
- treat Vault support as a fallback path for broad legacy compatibility only

## Long-Term Direction

The long-term goal is for Coffers to become a stronger default economy target for new integrations.

That does not require removing Vault compatibility.

It means:

- Coffers should stand on its own
- Coffers should be useful even when Vault is not installed
- Coffers-native integrations should become the best and most capable path

## Related Pages

- [Home](Home)
- [Vault Compatibility](Vault-Compatibility)
- [Developer API](Developer-API)
