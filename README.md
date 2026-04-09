# Coffers

Coffers is a fresh, from-scratch economy platform for modern Minecraft servers.

The direction is:

- Keep the good part of Vault: a stable abstraction other plugins can depend on.
- Avoid the old Vault trap: baking every integration directly into one giant plugin.
- Leave room for newer ideas like richer transactions, multi-currency, async storage, and cleaner bridges.

## What server owners install

Server owners only need one plugin jar:

- `Coffers.jar`

Vault compatibility is built into the main plugin and can be set to:

- `auto`: register with Vault only when Vault is installed
- `enabled`: require Vault compatibility behavior
- `disabled`: never register the Vault bridge

## Project layout

- `coffers-api`: shared developer-facing API definitions
- `coffers-paper`: the real Paper plugin, including the built-in Vault bridge

## Early goals

- Provide a clean economy service for Aegis Guard and future plugins.
- Keep Vault support as a built-in bridge, not the center of the design.
- Start with a small, understandable baseline before adding persistence and advanced features.

## What informed the design

These ideas were borrowed conceptually, not by copying code:

- Vault: a central abstraction that many plugins know how to consume.
- Treasury: keeping API concerns separate from implementation concerns.
- Newer Vault refreshes: trimming dead integrations and focusing on modern servers.

## Next milestones

- Replace the in-memory ledger with SQLite/MySQL storage.
- Add transaction history and audit metadata.
- Add configurable currencies and formatting rules.
- Add migration helpers for existing Vault economy setups.
- Publish the API jar separately for developers who want to build directly against Coffers.
