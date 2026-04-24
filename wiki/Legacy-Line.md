# Legacy Line

This page explains what the Coffers Legacy line is, who it is for, and how it differs from the main modern Coffers line.

## Overview

Coffers currently ships in two server-owner lines:

- `Coffers.jar`
- `Coffers-Legacy.jar`

The purpose of the legacy line is simple:

- support older server environments more safely
- avoid pretending one jar can cleanly support every Minecraft server generation
- keep the Coffers project usable for server owners who stay on older versions for plugin or mod compatibility reasons

## What Is Coffers Legacy?

Coffers Legacy is the older-server-oriented Coffers line.

It is intended for:

- older Spigot servers
- older Paper servers
- older CraftBukkit-style environments
- environments that cannot move to the modern Paper-focused line yet

The legacy line keeps the same general Coffers direction:

- built-in Vault compatibility
- configurable currencies
- YAML, SQLite, and MySQL storage
- transaction history
- migration helpers
- rollback tools
- backup, export, import, and restore support
- restore preview, account restore, and validation reports
- automatic safety backups and startup or shutdown safety copies

## Why The Legacy Line Exists

Not every server can upgrade to the latest Minecraft version or the latest Paper environment.

Some servers stay on older versions because of:

- mod compatibility
- plugin compatibility
- long-running network infrastructure
- performance or deployment constraints

Instead of forcing everything into one jar, Coffers uses a separate legacy line so older servers can have their own supported path.

## Which Jar Should You Use?

Use:

- `Coffers.jar` for the modern line
- `Coffers-Legacy.jar` for the legacy line

The legacy jar is for administrators who need the older-server-oriented Coffers build.

## Intended Compatibility

The legacy line is meant for older Bukkit-family setups.

Recommended use:

- `1.16.x`
- `1.17.x`

Important:

- do not assume the legacy line automatically supports every old Minecraft version ever released
- use the legacy line when you need the older-compatible Coffers build, not when you want the modern Paper-focused build

## What Stays the Same

The legacy line keeps many of the same major ideas as the modern line:

- built-in Vault compatibility
- storage backend support
- configurable currencies
- transaction history and audit-style records
- migration helpers for Vault-backed providers
- recovery tooling and archive workflows
- optional personal banks

This means the legacy line is still very much part of the Coffers project, not a completely unrelated plugin.

## What Differs From the Modern Line

The legacy line differs from the modern line in a few important ways:

- it targets an older server/API baseline
- it uses its own plugin jar
- it uses its own command root
- it uses its own permission prefix
- it uses its own default storage filenames and database names

Examples:

- modern command root: `/coffers`
- legacy command root: `/cofferslegacy`

- modern permission prefix: `coffers.command.*`
- legacy permission prefix: `cofferslegacy.command.*`

- modern SQLite default: `coffers.db`
- legacy SQLite default: `coffers-legacy.db`

## Legacy Commands

The legacy line uses:

- `/cofferslegacy balance [player] [currency]`
- `/cofferslegacy pay <player> <amount> [currency]`
- `/cofferslegacy paytoggle [on|off]`
- `/cofferslegacy set <player> <amount> [currency]`
- `/cofferslegacy history [player] [limit]`
- `/cofferslegacy rollback <entry-id>`
- `/cofferslegacy bank ...`
- `/cofferslegacy status`
- `/cofferslegacy backup [name]`
- `/cofferslegacy export [name]`
- `/cofferslegacy import <name>`
- `/cofferslegacy restore [source]`
- `/cofferslegacy restorepreview <source>`
- `/cofferslegacy restoreaccount <source> <target> [dryrun]`
- `/cofferslegacy validate`
- `/cofferslegacy currencies`
- `/cofferslegacy migratevault [provider]`

## Legacy Permissions

The legacy line uses its own permission nodes:

- `cofferslegacy.command.use`
- `cofferslegacy.command.balance.others`
- `cofferslegacy.command.set`
- `cofferslegacy.command.migratevault`
- `cofferslegacy.command.restore`
- `cofferslegacy.command.restorepreview`
- `cofferslegacy.command.restoreaccount`
- `cofferslegacy.command.validate`

## Legacy Storage Defaults

The legacy line keeps the same major backend choices:

- `yaml`
- `sqlite`
- `mysql`

But some default names differ from the modern line.

Examples:

- `legacy-accounts.yml`
- `legacy-history.yml`
- `coffers-legacy.db`
- `coffers_legacy`

This helps keep legacy data separate from modern data by default.

The legacy line also keeps its own backup, export, report, and registry files so recovery workflows stay separated from the modern line.

## Vault Compatibility on the Legacy Line

Coffers Legacy includes built-in Vault compatibility, just like the modern line.

Vault bridge modes:

- `auto`
- `enabled`
- `disabled`

The legacy line can register itself as a Vault economy provider so older plugin ecosystems still have a familiar economy bridge.

Vault remains optional. If your plugin stack no longer needs it, Coffers Legacy can still run standalone.

## Migration on the Legacy Line

The legacy line can migrate balances from another Vault-backed provider using:

- `/cofferslegacy migratevault`
- `/cofferslegacy migratevault <provider>`

This helps older servers transition into Coffers Legacy without needing to rebuild balances manually.

## When To Choose Coffers Legacy

Use Coffers Legacy when:

- your server is built around an older server baseline
- you depend on older plugins or infrastructure
- you want Coffers features without moving to the modern line
- you want the same safety and restore tooling while staying on a legacy server generation

Do not use Coffers Legacy just because it sounds safer.

If you are already on the intended modern Paper environment, use the main `Coffers.jar` line instead.

## Recommended Strategy

If you are unsure which line to use:

- choose `Coffers.jar` for the modern line
- choose `Coffers-Legacy.jar` for older `1.16.x` to `1.17.x` style environments

If you are planning a future upgrade path:

- start with the line that matches your current environment
- migrate carefully
- do not assume configuration and storage can be mixed without planning

## Summary

Coffers Legacy exists so older server owners are not left behind.

It gives older environments their own Coffers line while keeping the same overall project direction:

- economy management
- Vault compatibility
- storage flexibility
- configurable currencies
- migration support

## Next Step

Continue to:

- [FAQ](FAQ)
- [Home](Home)
- [Installation](Installation)
