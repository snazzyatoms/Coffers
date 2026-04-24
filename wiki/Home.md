# Coffers

Coffers is a standalone-first economy platform for Minecraft servers.

It is designed for server owners who want a complete economy plugin with its own storage, ledger history, recovery tooling, and optional Vault compatibility when older plugins still need it.

## What Is Coffers?

Coffers is built to provide:

- standalone economy support with no Vault requirement
- optional Vault compatibility for older plugin stacks
- configurable currencies and formatting rules
- ledger history with audit metadata and rollback tools
- YAML, SQLite, and MySQL storage
- named backups, exports, imports, and live restore tools
- restore preview, account restore, and validation reports
- optional personal bank accounts
- automatic scheduled safety backups with retention rules
- separate modern and legacy plugin lines
- native API and SPI integration paths for developers

## Which Jar Should You Use?

### Modern line

- Plugin: `Coffers.jar`
- Intended for modern Paper-based server environments
- Includes PlaceholderAPI integration when PlaceholderAPI is installed

### Legacy line

- Plugin: `Coffers-Legacy.jar`
- Intended for older Bukkit-family server environments
- Designed for the `1.16.x` to `1.17.x` range and comparable legacy-oriented setups

### Developer modules

- `Coffers-API.jar`
- `Coffers-SPI.jar`

These are for developers and integration work, not for normal server installation as the main economy jar.

## Why v0.2 Matters

Starting with `v0.2`, Coffers is more than a basic balance plugin.

The project now includes:

- rollback tools for transaction correction
- bank support for administrative and player-facing savings workflows
- named backup and export files
- direct restore without moving files around or restarting the server
- restore preview and account-specific recovery tools
- validation reports for currency and account integrity checks
- startup and shutdown safety copies
- scheduled safety backups with configurable intervals such as `15`, `30`, or `60` minutes

## Quick Start

1. Download the correct jar for your server.
2. Place it in your server `plugins` folder.
3. Start the server once to generate config files.
4. Review `storage`, `compatibility`, `currencies`, and `safety` settings.
5. Restart the server after making changes.
6. Use `/coffers` or `/cofferslegacy` to manage the economy.

## Recommended Reading

- [Installation](Installation)
- [Configuration](Configuration)
- [Storage Backends](Storage-Backends)
- [Vault Compatibility](Vault-Compatibility)
- [Commands](Commands)
- [Migration Guide](Migration-Guide)
- [Legacy Line](Legacy-Line)
- [Developer API](Developer-API)
- [FAQ](FAQ)

## For Server Owners

If you just want to run Coffers:

- use `Coffers.jar` for the modern line
- use `Coffers-Legacy.jar` for older server environments
- choose YAML, SQLite, or MySQL depending on your server size and infrastructure
- leave Vault compatibility on `auto` unless you have a reason to change it
- review the `safety` section in config so backup intervals and retention match your server

## For Developers

If you want to integrate another plugin with Coffers:

- use `Coffers-API.jar` for rich direct integrations
- use `Coffers-SPI.jar` for shared native service-style contracts
- treat Vault mainly as compatibility support for older ecosystems

## Source Code

- GitHub repository: [Coffers](https://github.com/snazzyatoms/Coffers)

## License

Coffers is currently intended to use the Apache-2.0 license.

---

Coffers is built to give server owners a safer, more recoverable economy platform while still respecting real-world compatibility needs.
