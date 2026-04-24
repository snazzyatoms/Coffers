# Commands and Permissions

This page covers the commands and permission nodes for both the modern Coffers line and the Coffers Legacy line.

## Command Roots and Aliases

Coffers provides:

- modern command root: `/coffers`
- legacy command root: `/cofferslegacy`
- shared top-balance alias: `/baltop`
- shared payment toggle alias: `/paytoggle`

Both lines support the same major command families:

- `balance`
- `pay`
- `paytoggle`
- `set`
- `history`
- `rollback`
- `bank`
- `status`
- `top`
- `currencies`
- `migratevault`
- `reload`
- `backup`
- `export`
- `import`
- `restore`
- `restorepreview`
- `restoreaccount`
- `validate`

## Modern Commands

The modern line uses `/coffers`.

### Economy Basics

- `/coffers balance [player] [currency]`
- `/coffers pay <player> <amount> [currency]`
- `/coffers paytoggle [on|off]`
- `/coffers set <player> <amount> [currency]`
- `/coffers currencies`
- `/coffers top [currency] [limit]`
- `/baltop [currency] [limit]`

Notes:

- `balance` without a player argument checks your own balance
- `paytoggle` lets a player allow or block incoming player payments
- `baltop` is an alias for the standard top-balance view
- checking another player's balance requires `coffers.command.balance.others`

### History and Corrections

- `/coffers history [player] [page=<page>] [limit=<limit>] [currency=<id>] [kind=<kind>]`
- `/coffers rollback <entry-id>`

Examples:

- `/coffers history`
- `/coffers history Kai`
- `/coffers history Kai page=2 limit=10`
- `/coffers history Kai currency=coins kind=deposit`
- `/coffers rollback 5ec7d8d8-...`

Notes:

- history filters can be combined
- `rollback` creates a correction entry using the Coffers ledger rather than silently editing history
- viewing another player's history requires `coffers.command.history.others`

### Bank Commands

- `/coffers bank create <name>`
- `/coffers bank delete <name>`
- `/coffers bank balance <name> [currency]`
- `/coffers bank deposit <name> <amount> [currency]`
- `/coffers bank withdraw <name> <amount> [currency]`
- `/coffers bank history <name> [page=<page>] [limit=<limit>] [currency=<id>] [kind=<kind>]`

Notes:

- bank activity is stored in the same Coffers ledger system as player accounts
- optional personal player banks can be auto-created on join through config
- bank actions use dedicated permission nodes

### Status and Administration

- `/coffers status`
- `/coffers reload`
- `/coffers migratevault [provider]`

`/coffers status` includes:

- active storage backend
- default currency
- configured currency count
- Vault bridge mode
- PlaceholderAPI status
- bank count
- blocked payment count
- automated backup state
- automated backup interval
- automated backup retention

### Safety and Recovery

- `/coffers backup [name]`
- `/coffers export [name]`
- `/coffers import <name>`
- `/coffers restore [latest|backup-name|export:name]`
- `/coffers restorepreview <latest|backup-name|export:name>`
- `/coffers restoreaccount <source> <player|uuid|bank:name> [dryrun]`
- `/coffers validate`

Examples:

- `/coffers backup pre-release`
- `/coffers export migration-copy`
- `/coffers import migration-copy`
- `/coffers restore latest`
- `/coffers restore export:migration-copy`
- `/coffers restorepreview latest`
- `/coffers restoreaccount latest Kai dryrun`
- `/coffers restoreaccount export:migration-copy bank:TownTreasury`
- `/coffers validate`

Notes:

- `backup` writes to the plugin `backups` directory
- `export` writes to the plugin `exports` directory
- `import` reads from `exports`
- `restore` can read from named backups or explicit export sources
- `restorepreview` creates a recovery report without changing live data
- `restoreaccount` can restore a single player account or a single bank account
- `validate` writes a validation report for currency, account, history, and bank consistency
- backups, exports, imports, and restores include balances, history, pay-toggle state, and bank registry data

## Legacy Commands

The legacy line uses `/cofferslegacy`.

The legacy line mirrors the modern command set with the legacy root:

- `/cofferslegacy balance [player] [currency]`
- `/cofferslegacy pay <player> <amount> [currency]`
- `/cofferslegacy paytoggle [on|off]`
- `/cofferslegacy set <player> <amount> [currency]`
- `/cofferslegacy history [player] [page=<page>] [limit=<limit>] [currency=<id>] [kind=<kind>]`
- `/cofferslegacy rollback <entry-id>`
- `/cofferslegacy bank ...`
- `/cofferslegacy status`
- `/cofferslegacy top [currency] [limit]`
- `/cofferslegacy currencies`
- `/cofferslegacy migratevault [provider]`
- `/cofferslegacy reload`
- `/cofferslegacy backup [name]`
- `/cofferslegacy export [name]`
- `/cofferslegacy import <name>`
- `/cofferslegacy restore [latest|backup-name|export:name]`
- `/cofferslegacy restorepreview <latest|backup-name|export:name>`
- `/cofferslegacy restoreaccount <source> <player|uuid|bank:name> [dryrun]`
- `/cofferslegacy validate`

Notes:

- the recovery and safety flow works the same way on the legacy line
- `/baltop` and `/paytoggle` also work with the legacy line when it is the active jar
- legacy uses its own permission namespace: `cofferslegacy.command.*`

## Permission Overview

The modern and legacy lines use separate permission namespaces.

### Modern permissions

- `coffers.command.use`
- `coffers.command.balance`
- `coffers.command.pay`
- `coffers.command.paytoggle`
- `coffers.command.pay.ignore-toggle`
- `coffers.command.history`
- `coffers.command.top`
- `coffers.command.currencies`
- `coffers.command.status`
- `coffers.command.balance.others`
- `coffers.command.history.others`
- `coffers.command.set`
- `coffers.command.rollback`
- `coffers.command.bank.create`
- `coffers.command.bank.delete`
- `coffers.command.bank.balance`
- `coffers.command.bank.deposit`
- `coffers.command.bank.withdraw`
- `coffers.command.bank.history`
- `coffers.command.migratevault`
- `coffers.command.reload`
- `coffers.command.backup`
- `coffers.command.export`
- `coffers.command.import`
- `coffers.command.restore`
- `coffers.command.restorepreview`
- `coffers.command.restoreaccount`
- `coffers.command.validate`

### Legacy permissions

- `cofferslegacy.command.use`
- `cofferslegacy.command.balance`
- `cofferslegacy.command.pay`
- `cofferslegacy.command.paytoggle`
- `cofferslegacy.command.pay.ignore-toggle`
- `cofferslegacy.command.history`
- `cofferslegacy.command.top`
- `cofferslegacy.command.currencies`
- `cofferslegacy.command.status`
- `cofferslegacy.command.balance.others`
- `cofferslegacy.command.history.others`
- `cofferslegacy.command.set`
- `cofferslegacy.command.rollback`
- `cofferslegacy.command.bank.create`
- `cofferslegacy.command.bank.delete`
- `cofferslegacy.command.bank.balance`
- `cofferslegacy.command.bank.deposit`
- `cofferslegacy.command.bank.withdraw`
- `cofferslegacy.command.bank.history`
- `cofferslegacy.command.migratevault`
- `cofferslegacy.command.reload`
- `cofferslegacy.command.backup`
- `cofferslegacy.command.export`
- `cofferslegacy.command.import`
- `cofferslegacy.command.restore`
- `cofferslegacy.command.restorepreview`
- `cofferslegacy.command.restoreaccount`
- `cofferslegacy.command.validate`

## Recommended Permission Setup

For most servers:

- leave `*.command.use`, `*.command.balance`, `*.command.pay`, `*.command.paytoggle`, `*.command.top`, and `*.command.currencies` available to normal players
- reserve `balance.others`, `history.others`, `set`, `rollback`, `bank.*`, `migratevault`, `reload`, `backup`, `export`, `import`, `restore`, `restorepreview`, `restoreaccount`, and `validate` for staff or administrators
- only grant `pay.ignore-toggle` to trusted staff roles if you want them to bypass a player's payment preference

## Recovery Workflow Tip

For the safest admin workflow:

1. run a named backup before risky changes
2. use `restorepreview` before applying a full restore
3. use `restoreaccount` when only one player or bank needs recovery
4. run `validate` after major migration or recovery work

## Next Step

Continue to:

- [Configuration](Configuration)
- [Storage Backends](Storage-Backends)
- [Vault Compatibility](Vault-Compatibility)
- [Migration Guide](Migration-Guide)
