# Migration Guide

This page explains how to migrate balances into Coffers from another Vault-backed economy provider.

## Overview

Coffers includes migration helpers for servers that are already using another economy plugin through Vault.

This is useful when you want to:

- replace an older economy plugin with Coffers
- preserve balances during a plugin transition
- move from a legacy provider into the Coffers ecosystem

Both the modern and legacy lines include migration commands.

## Supported Migration Style

Coffers currently migrates from:

- another economy provider already registered through Vault on the same server

This means Coffers does not need a custom importer for every economy plugin individually.

Instead, it reads balances from a Vault-backed provider that is already available on the server.

## Important Before You Begin

Before migrating:

- back up your server
- run a named Coffers backup before each migration attempt
- keep a copy of your existing economy data
- confirm that Vault is installed if your current economy depends on Vault
- confirm that the old provider is visible through Vault
- make sure you know whether you are migrating into the modern line or the legacy line

Recommended:

- test migration on a copy of the server first
- verify balances after migration before making permanent changes
- keep automatic safety backups enabled during migration work

## Migration Commands

### Modern line

Use:

- `/coffers migratevault`
- `/coffers migratevault <provider>`

### Legacy line

Use:

- `/cofferslegacy migratevault`
- `/cofferslegacy migratevault <provider>`

## How Provider Selection Works

If you do not specify a provider:

- Coffers selects the highest-priority external Vault economy provider it can find

If you do specify a provider:

- Coffers tries to match the provider name or plugin name you give

Example:

- `/coffers migratevault Essentials`

If the provider cannot be found:

- the migration will fail with a message
- Coffers will show the list of available providers when possible

## What Gets Migrated

Coffers migration currently imports:

- player balances from the source Vault provider

The migration writes those balances into:

- the default Coffers currency

This is important:

- migration is centered on the default Coffers currency
- Vault-based migration does not automatically map advanced multi-currency setups from other systems

## What Happens During Migration

For each offline player known to the server, Coffers checks:

- whether the source provider has an account
- the player's current balance in the source provider
- the player's current balance in Coffers

If the balance differs:

- Coffers sets the Coffers balance to match the source provider
- a migration transaction is recorded with audit metadata

If the balance is already the same:

- the account is skipped

## Migration Reporting

After migration, Coffers reports:

- provider name
- imported accounts
- updated accounts
- skipped accounts

This helps you confirm whether the migration actually changed balances or only checked them.

You can also use Coffers' safety tooling around migration:

- run `/coffers backup pre-migration` or `/cofferslegacy backup pre-migration`
- use `/coffers validate` or `/cofferslegacy validate` after migration
- use restore preview or targeted restore if you need to inspect or recover individual accounts afterward

## Recommended Migration Process

### Step 1

Back up all current economy-related data.

Recommended:

- `/coffers backup pre-migration`
- or `/cofferslegacy backup pre-migration`

### Step 2

Install Coffers and make sure it starts correctly.

### Step 3

Leave the old Vault-backed provider available so Coffers can read from it.

### Step 4

Run:

- `/coffers migratevault`

or:

- `/coffers migratevault <provider>`

Use the legacy command instead if you are working on the legacy line.

### Step 5

Review the migration output and verify balances manually.

Optional extra safety:

- run `/coffers validate`
- or `/cofferslegacy validate`

### Step 6

Only after confirming results, decide whether to disable or remove the old provider from active use.

## Permission Requirements

Modern line:

- `coffers.command.migratevault`

Legacy line:

- `cofferslegacy.command.migratevault`

These permissions should generally be restricted to administrators.

## Best Practices

Recommended migration habits:

- migrate during a maintenance window
- back up before every migration attempt
- test on a copy of the server first
- verify balances for a sample of real player accounts
- confirm Vault provider selection before finalizing the change

## Common Migration Mistakes

Avoid these common issues:

- trying to migrate without Vault installed when the old provider depends on Vault
- uninstalling the old provider before Coffers can read from it
- assuming multi-currency mappings happen automatically
- skipping backups before migration
- migrating on a live production server without checking the result
- forgetting that Coffers can now restore from backups directly if you need to recover

## Modern vs Legacy Migration

The migration concept is the same on both lines:

- both use Vault-backed provider discovery
- both import into the default Coffers currency
- both record migration activity in their own economy system

The main difference is simply which plugin line you are using:

- `Coffers.jar` uses `/coffers migratevault`
- `Coffers-Legacy.jar` uses `/cofferslegacy migratevault`

## What Migration Does Not Do

Coffers migration does not currently guarantee:

- automatic multi-currency mapping from another economy system
- migration of Vault bank accounts
- plugin-specific custom data outside normal balance reads

If your old provider stored special data outside the standard Vault economy interface, that data may require manual handling.

## Summary

If your old economy plugin is available through Vault, Coffers can help you import balances into its own system with built-in migration commands.

The safest path is:

1. back up everything
2. install Coffers
3. keep the old Vault provider available
4. run migration
5. verify balances
6. run validation if needed
7. then retire the old setup

## Next Step

Continue to:

- [Currencies](Currencies)
- [Developer API](Developer-API)
- [Legacy Line](Legacy-Line)
- [FAQ](FAQ)
