# FAQ

This page answers common questions about Coffers, Coffers Legacy, Vault compatibility, storage, backup safety, and the developer modules.

## What is Coffers?

Coffers is a standalone-first economy platform for Minecraft servers.

It combines:

- live economy storage
- configurable currencies
- ledger history and rollback tools
- recovery workflows such as backup, export, import, restore, restore preview, and validation reports
- optional Vault compatibility

## Is Coffers just Vault with a different name?

No.

Vault is mainly a compatibility abstraction layer.

Coffers is a full economy platform with:

- its own storage
- its own balances and bank registry
- its own ledger history
- recovery and restore tooling
- its own API and SPI modules

Vault compatibility is included, but it is not the whole product.

## Do I still need Vault?

Only if another plugin still expects the Vault plugin to exist.

In short:

- Coffers can register a Vault economy provider
- Coffers can run perfectly fine without Vault installed
- if another plugin explicitly depends on Vault, you still need Vault on the server

## Which jar should I install?

Use:

- `Coffers.jar` for the modern line
- `Coffers-Legacy.jar` for older server environments
- `Coffers-API.jar` or `Coffers-SPI.jar` only if you are a developer building direct integrations

Normal server owners should not install the API or SPI jars as their main economy plugin.

## What is Coffers Legacy?

Coffers Legacy is the older-server-oriented Coffers line.

Use it for older Bukkit-family server environments, especially in the `1.16.x` to `1.17.x` range.

## Does one jar support every Minecraft version?

No.

Coffers intentionally uses separate modern and legacy lines instead of pretending one jar safely supports every server generation.

## What storage backends are supported?

Coffers supports:

- `yaml`
- `sqlite`
- `mysql`

These storage options are available in both the modern and legacy lines.

## Which storage backend should I use?

General recommendation:

- use `yaml` for smaller and simpler servers
- use `sqlite` for a strong local single-server setup
- use `mysql` for larger or shared deployments

## Can Coffers use multiple currencies?

Yes.

Coffers supports configurable currencies, including:

- IDs
- singular and plural names
- symbols
- fractional digits
- starting balances
- formatting rules

Vault-based compatibility still centers on the default Coffers currency.

## Does Coffers have bank support?

Yes.

Coffers includes built-in bank support for player and administrative workflows.

Banks are stored in Coffers itself and are included in backups, exports, imports, and restore operations.

## Does Coffers support bank accounts through Vault?

Not through the Vault bank API.

Important distinction:

- Coffers has its own built-in bank system
- Coffers does not currently expose Vault bank operations as a full Vault bank implementation

If a third-party plugin specifically depends on Vault bank calls, you should test that workflow carefully.

## Can Coffers protect player balances if something goes wrong?

That is one of the major goals of `v0.2`.

Coffers now includes:

- persistent balance storage
- ledger history
- rollback tools
- named backups
- named exports and imports
- live restore
- restore preview
- targeted player or bank restore
- validation reports
- scheduled safety backups
- startup and shutdown safety copies

This means Coffers is designed to be easier to recover from mistakes, bad migrations, or runtime issues than a simple balance-only system.

## Are player banks the backup system?

No.

Player banks are an economy feature, not the only recovery layer.

The real safety model is:

- persistent storage
- ledger history
- rollback tools
- snapshots and restore tools

Banks can still provide peace of mind and organization, but they are not meant to replace backups.

## Can I migrate from another economy plugin?

Yes, if that plugin is exposed through Vault as an economy provider.

Coffers includes migration helpers for importing balances from another Vault-backed provider already present on the server.

Commands:

- `/coffers migratevault`
- `/coffers migratevault <provider>`
- `/cofferslegacy migratevault`
- `/cofferslegacy migratevault <provider>`

## What does migration import?

Coffers currently imports:

- player balances from another Vault-backed provider

Migration writes those balances into the default Coffers currency.

It does not automatically guarantee advanced multi-currency mapping or plugin-specific custom data migration.

## Can I switch storage backends later?

Yes, but carefully.

You should always:

- back up existing data
- understand that data is not automatically shared between backends
- verify balances after switching

Switching from YAML to SQLite or MySQL should be treated as a real data transition.

## Does Coffers support PlaceholderAPI?

Yes, on the modern line.

The modern line includes an optional PlaceholderAPI expansion when PlaceholderAPI is installed.

## Should developers use the Coffers API, the SPI, or Vault?

Recommended:

- use the Coffers API for rich direct integrations
- use the Coffers SPI for shared native service-style integration contracts
- use Vault only when you need broad compatibility with older plugin ecosystems

## Is Coffers tested?

The project currently includes automated test coverage for:

- API defaults
- modern economy behavior
- modern archive flows
- legacy economy behavior
- legacy archive flows
- bank registry behavior

The project also passes:

- `mvn clean test package`

Live server testing is still important before production rollout.

## Is Coffers open source?

The project is intended to use:

- `Apache-2.0`

## Where can I find the source code?

Source code is available at:

- [https://github.com/snazzyatoms/Coffers](https://github.com/snazzyatoms/Coffers)

## Where should I start in the wiki?

Recommended reading order:

1. [Home](Home)
2. [Installation](Installation)
3. [Configuration](Configuration)
4. [Commands](Commands)
5. [Vault Compatibility](Vault-Compatibility)
6. [Storage Backends](Storage-Backends)
7. [Migration Guide](Migration-Guide)
8. [Legacy Line](Legacy-Line)
9. [Developer API](Developer-API)
