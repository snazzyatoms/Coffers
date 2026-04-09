# Coffers Legacy

Coffers Legacy is the standalone older-server line for Coffers.

This folder is intentionally separate from the main modern Coffers implementation so we can port features carefully for older Spigot/Paper/Purpur server versions without breaking the main plugin line.

## Goal

- target older Minecraft server versions
- support legacy-friendly Java and server API baselines
- keep the same Coffers identity where possible
- provide a separate legacy jar for older server owners
- keep Vault support available only as an optional compatibility bridge

## Current Status

This module is part of the Maven build and currently produces:

- `release/Coffers-Legacy.jar`

The legacy line is intended for server owners who need older Spigot/Paper/Purpur compatibility without moving to the modern Paper-focused Coffers jar.

## Current Direction

- target an older API baseline than the modern Coffers line
- keep storage options such as YAML, SQLite, and MySQL available
- keep Vault compatibility available for legacy plugin ecosystems
- continue aligning features with the main Coffers project where practical
