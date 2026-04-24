# Installation

This page explains how to install Coffers, choose the correct jar, and prepare the plugin for safe production use.

## Choose the Correct Jar

Coffers currently ships in four artifacts:

- `Coffers.jar`
  the modern server jar
- `Coffers-Legacy.jar`
  the legacy server jar for older Bukkit-family environments
- `Coffers-API.jar`
  the developer API jar
- `Coffers-SPI.jar`
  the service-provider interface jar for native integration contracts

## Which One Should You Use?

Use the jar that matches your server:

- use `Coffers.jar` for the modern line
- use `Coffers-Legacy.jar` for older server environments, especially in the `1.16.x` to `1.17.x` range
- do not install `Coffers-API.jar` or `Coffers-SPI.jar` as your main economy plugin

## Basic Installation Steps

1. Download the correct jar for your server.
2. Stop the server if it is currently running.
3. Place the jar into your server `plugins` folder.
4. Start the server once so Coffers can generate its default files.
5. Open the generated configuration file and review `storage`, `currencies`, `compatibility`, and `safety`.
6. Restart the server after making configuration changes.

## First Startup Checklist

On first startup, Coffers will create its plugin data folder and default configuration files.

After the first boot, review:

- the configured storage backend
- the default currency and enabled currencies
- Vault bridge mode
- whether player banks should auto-create on join
- automatic backup interval and retention
- whether startup and shutdown safety copies should stay enabled

## Recommended Safety Defaults

For most servers, it is a good idea to leave the following enabled:

- scheduled safety backups
- startup safety copy
- shutdown safety copy

Common automatic backup intervals:

- `15` minutes
- `30` minutes
- `60` minutes

These settings help protect balances, bank data, payment preferences, and history if something goes wrong.

## Storage Setup

Coffers supports:

- `yaml`
- `sqlite`
- `mysql`

Recommended usage:

- use `yaml` for simple and lightweight servers
- use `sqlite` for strong single-server persistence
- use `mysql` for larger or shared deployments

The modern and legacy lines each have their own configuration files and storage paths.

## Vault Compatibility

Coffers includes built-in Vault compatibility.

You do not need a separate Coffers-to-Vault bridge plugin.

Available modes:

- `auto`
- `enabled`
- `disabled`

For most servers, `auto` is the correct choice.

## Common Mistakes

Avoid these setup problems:

- installing `Coffers-API.jar` or `Coffers-SPI.jar` instead of the real server plugin jar
- using the modern jar on a legacy environment
- using the legacy jar on a modern environment when you intended to use the main line
- disabling all safety backups without having another recovery plan
- forgetting to restart after changing storage or currency settings

## Next Step

After installation, continue to:

- [Configuration](Configuration)
- [Commands](Commands)
- [Storage Backends](Storage-Backends)
- [Vault Compatibility](Vault-Compatibility)
