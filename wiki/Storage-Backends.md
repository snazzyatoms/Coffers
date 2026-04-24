# Storage Backends

This page explains the available storage backends in Coffers and Coffers Legacy, when to use each one, and what to consider before choosing a storage mode.

## Overview

Coffers supports three storage backend types:

- `yaml`
- `sqlite`
- `mysql`

These are available in both:

- `Coffers.jar`
- `Coffers-Legacy.jar`

The active backend is selected through the config:

```yml
storage:
  type: yaml
```

## Why Storage Matters

Your storage backend determines:

- where balances are saved
- where transaction history is stored
- where bank account data is persisted
- where payment-toggle preferences are backed up from
- how easy the plugin is to manage
- how well the economy setup fits a small server versus a larger deployment

Choosing the right backend depends on your server size, infrastructure, and how much persistence and flexibility you need.

## YAML Storage

YAML is the simplest storage option.

It stores balances and history in local configuration files inside the plugin data folder.

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

### Best For

- small servers
- simple installations
- administrators who want the least setup work
- local-only economy storage

### Advantages

- easiest to set up
- no SQL server required
- simple file-based storage
- good for smaller communities or test servers

### Tradeoffs

- not ideal for larger deployments
- less structured than SQL storage
- file-based storage is not the best fit for complex scaling needs

## SQLite Storage

SQLite stores data in a local database file on the server.

Modern line example:

```yml
storage:
  type: sqlite
  sqlite:
    file: coffers.db
```

Legacy line example:

```yml
storage:
  type: sqlite
  sqlite:
    file: coffers-legacy.db
```

### Best For

- single-server production use
- admins who want more structured persistence without running MySQL
- servers that need a stronger storage format than YAML

### Advantages

- local database storage
- no external database server required
- cleaner structure than flat file storage
- very good option for one-server setups

### Tradeoffs

- still local to one machine
- not the right fit for shared multi-server database setups

## MySQL Storage

MySQL is intended for larger setups or environments that want shared SQL storage.

Modern line example:

```yml
storage:
  type: mysql
  mysql:
    host: localhost
    port: 3306
    database: coffers
    username: root
    password: change-me
    parameters: useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
```

Legacy line example:

```yml
storage:
  type: mysql
  mysql:
    host: localhost
    port: 3306
    database: coffers_legacy
    username: root
    password: change-me
    parameters: useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
```

### Best For

- larger servers
- advanced administrators
- environments that already use MySQL
- shared or centralized data infrastructure

### Advantages

- structured SQL persistence
- suitable for larger environments
- better fit for central database management

### Tradeoffs

- requires database setup and credentials
- more moving parts than YAML or SQLite
- configuration mistakes can prevent startup

## Recommended Storage Choices

### Small server

Recommended backend:

- `yaml`

Why:

- simple
- quick to configure
- no database setup required

### Single-server production

Recommended backend:

- `sqlite`

Why:

- stronger persistence than YAML
- no external database required
- good middle ground for many servers

### Larger or shared infrastructure

Recommended backend:

- `mysql`

Why:

- better for central database management
- better fit for more advanced deployments

## Transaction History Storage

The selected backend stores not only balances, but also account history.

This includes:

- account balances per currency
- recent ledger entries
- transaction audit metadata

Around that storage layer, Coffers also tracks:

- bank registry data
- player payment preferences
- recovery archives such as backups and exports

The amount of history retained per account is controlled separately through:

```yml
history:
  max-per-account: 50
```

## Switching Storage Backends

If you change storage type after using Coffers for a while, keep in mind:

- existing data does not automatically move unless you migrate it yourself
- you should back up your current data before changing storage mode
- switching from one backend to another without planning may make old balances appear missing

Best practice:

1. stop the server
2. back up the current Coffers data
3. change the storage type in config
4. move or import data if needed
5. restart and verify balances
6. run `validate` if you want a post-change integrity report

## Common Mistakes

Avoid these common storage setup problems:

- using MySQL without valid credentials
- changing storage mode without backing up existing data
- assuming YAML, SQLite, and MySQL share data automatically
- forgetting that modern and legacy lines use different default file names and database names

## Modern and Legacy Differences

The storage model is similar between both lines, but the defaults differ slightly.

Examples:

- modern YAML files use names like `accounts.yml` and `history.yml`
- legacy YAML files use names like `legacy-accounts.yml` and `legacy-history.yml`
- modern SQLite defaults to `coffers.db`
- legacy SQLite defaults to `coffers-legacy.db`
- modern MySQL defaults to the `coffers` database name
- legacy MySQL defaults to the `coffers_legacy` database name

## Summary

If you want the simplest possible setup:

- use `yaml`

If you want the best local production choice for one server:

- use `sqlite`

If you want a larger or more advanced deployment:

- use `mysql`

## Next Step

Continue to:

- [Currencies](Currencies)
- [Migration Guide](Migration-Guide)
- [Developer API](Developer-API)
- [FAQ](FAQ)
