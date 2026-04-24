# Vault Compatibility

This page explains how Coffers works with Vault in both the modern line and the legacy line.

Important direction note:

- Coffers is designed to run without Vault
- Vault support is included as a built-in compatibility bridge
- for new integrations, Coffers API is the preferred target instead of Vault

## Overview

Coffers includes built-in Vault compatibility.

That means:

- you do not need a separate Coffers-to-Vault bridge plugin
- Coffers can register itself as a Vault economy provider
- existing Vault-aware plugins can interact with Coffers through the Vault economy interface

This applies to both:

- `Coffers.jar`
- `Coffers-Legacy.jar`

## Why This Matters

Many Minecraft plugins were written to depend on Vault instead of directly integrating with a specific economy plugin.

By exposing a Vault-compatible economy provider, Coffers can work with those plugins while still using its own internal storage, transaction handling, and API design.

This is a compatibility feature, not the center of Coffers' long-term design.

## How Coffers Registers With Vault

Vault compatibility is controlled through the config:

```yml
compatibility:
  vault-bridge: auto
```

Available modes:

- `auto`
  Register with Vault only when Vault is installed

- `enabled`
  Force Coffers to expose a Vault economy provider when Vault is present

- `disabled`
  Never register the Vault bridge

If the config value is missing or invalid, Coffers falls back to `auto`.

## Recommended Setting

For most servers, use:

- `auto`

This gives you the safest default behavior:

- if Vault is installed, Coffers registers its provider
- if Vault is not installed, Coffers simply skips Vault registration

## When Vault Is Not Installed

If Vault is not present:

- Coffers still runs normally as its own economy plugin
- the Vault bridge is simply not registered
- plugins that require Vault will still need Vault installed on the server

Important:

- Coffers includes Vault compatibility
- Coffers does not replace the Vault plugin itself

If another plugin expects Vault to exist, you still need Vault installed.

## Modern Line Behavior

The modern line registers a Vault economy provider named:

- `Coffers`

This provider uses Coffers' default currency when responding through the Vault API.

That means Vault-aware plugins will interact with:

- the default Coffers balance
- the default Coffers currency

The Vault bridge uses Coffers internally for:

- account creation
- balance checks
- deposits
- withdrawals
- formatting
- currency name lookups

## Legacy Line Behavior

The legacy line registers a Vault economy provider named:

- `CoffersLegacy`

Like the modern line, it uses the default Coffers Legacy currency when responding through the Vault API.

This allows older Vault-based plugin ecosystems to work with the legacy Coffers line while keeping the same overall project direction.

## Bank Support

Coffers does not currently expose a full Vault bank implementation.

For both modern and legacy lines:

- built-in Coffers bank support is available inside Coffers itself
- archived banks can be backed up, exported, restored, and targeted for recovery
- Vault bank operations still return `NOT_IMPLEMENTED`

If a plugin specifically depends on Vault bank features, it may not behave as expected with Coffers at this stage.

## Currency Behavior Through Vault

Vault itself is primarily designed around a simpler economy model.

Because of that:

- Vault integrations use the default Coffers currency
- advanced Coffers multi-currency behavior is not fully exposed through the Vault layer
- richer Coffers economy features are available through direct Coffers integrations, not through the older Vault abstraction

This is one of the reasons Coffers includes both:

- Vault compatibility for legacy plugin ecosystems
- a separate Coffers API for richer direct integrations

## Migration From Existing Vault Providers

Coffers can also migrate balances from another Vault-backed economy provider already present on the server.

Modern line:

- `/coffers migratevault`
- `/coffers migratevault <provider>`

Legacy line:

- `/cofferslegacy migratevault`
- `/cofferslegacy migratevault <provider>`

This is useful when:

- moving from an older economy plugin to Coffers
- replacing an existing Vault provider with Coffers
- preserving balances during a transition

## Best Practices

Recommended usage:

- use `auto` for most setups
- keep Vault installed if your plugin stack depends on it
- migrate from older providers only after backing up your data
- use the Coffers API directly for richer integrations when possible
- rely on Vault compatibility mainly for older or third-party plugin support
- treat Vault as optional compatibility, not as the main way to understand Coffers

## Common Misunderstandings

### "Does Coffers replace Vault?"

No.

Coffers can register a Vault economy provider, but if another plugin requires the Vault plugin itself, Vault still needs to be installed.

### "Can Coffers run without Vault?"

Yes.

That is part of the intended standalone-first direction of the project.

Vault support is optional and should only be enabled when needed for compatibility.

### "Does Vault expose all Coffers features?"

No.

Vault support is mainly for legacy compatibility.

The full Coffers feature set, especially around richer metadata and future direct integration design, belongs to the Coffers API and Coffers internals.

### "Can Vault use multiple Coffers currencies?"

Not in the same way as direct Coffers integrations.

Vault support is centered on the default currency.

## Next Step

Continue to:

- [Migration Guide](Migration-Guide)
- [Developer API](Developer-API)
- [Standalone-First Direction](Standalone-First)
- [Configuration](Configuration)
- [Commands](Commands)
