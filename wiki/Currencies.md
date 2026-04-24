# Currencies

This page explains how currencies work in Coffers and Coffers Legacy, including default currency selection, formatting rules, and multi-currency setup.

## Overview

Coffers supports configurable currencies.

Each currency can define:

- an internal currency ID
- a singular display name
- a plural display name
- a symbol
- fractional digits
- a starting balance
- formatting rules

This gives Coffers a more flexible economy model than older single-currency-only designs.

## Where Currencies Are Configured

Currencies are defined in `config.yml` under the `currencies` section.

Modern line example:

```yml
currencies:
  default: coins
  definitions:
    coins:
      singular: coin
      plural: coins
      symbol: $
      fractional-digits: 2
      starting-balance: 100.00
      format:
        symbol-first: true
        space-between-symbol-and-amount: false
        space-between-amount-and-name: true
        use-grouping: true
        show-trailing-zeros: true
```

Legacy line example:

```yml
currencies:
  default: coins
  definitions:
    coins:
      singular: coin
      plural: coins
      symbol: $
      fractional-digits: 2
      starting-balance: 100.00
      format:
        symbol-first: true
        space-between-symbol-and-amount: false
        space-between-amount-and-name: true
        use-grouping: true
        show-trailing-zeros: true
```

## Currency IDs

Each currency is defined under a key such as:

- `coins`
- `gems`

That key becomes the currency ID used internally.

Example:

```yml
definitions:
  coins:
    singular: coin
    plural: coins
```

In this example:

- the currency ID is `coins`

Currency IDs are used in:

- config
- commands
- internal economy handling
- plugin integrations

## Default Currency

The default currency is configured here:

```yml
currencies:
  default: coins
```

Important:

- the default currency must exist in `currencies.definitions`
- Coffers validates this at startup
- if the default currency is missing, startup fails instead of silently using a broken configuration

This protects server owners from accidentally launching with an invalid currency setup.

## Currency Names

Each currency supports:

- `singular`
- `plural`

Example:

```yml
coins:
  singular: coin
  plural: coins
```

These names are used when Coffers formats balances for display.

## Currency Symbols

Each currency can define a symbol.

Example:

```yml
coins:
  symbol: $
```

Modern default sample currencies:

- `coins` uses `$`
- `gems` uses `◆`

Legacy default sample currencies:

- `coins` uses `$`
- `gems` uses `*`

This difference exists because the legacy line uses more conservative sample defaults.

## Fractional Digits

Each currency defines how many decimal places it supports.

Example:

```yml
coins:
  fractional-digits: 2
```

Common examples:

- `2` for currencies like dollars or coins with decimals
- `0` for currencies like gems or tokens with whole-number balances only

This affects:

- storage normalization
- display formatting
- command output
- Vault-facing fractional behavior for the default currency

## Starting Balance

Each currency can define the starting balance new accounts receive.

Example:

```yml
coins:
  starting-balance: 100.00
```

This is applied when Coffers creates an account for a player that does not already exist.

## Formatting Rules

Each currency can define its own formatting rules.

Example:

```yml
format:
  symbol-first: true
  space-between-symbol-and-amount: false
  space-between-amount-and-name: true
  use-grouping: true
  show-trailing-zeros: true
```

These settings control how balances are displayed.

### `symbol-first`

Controls whether the symbol comes before the amount.

Example:

- `$100 coins`
- `100 $ coins`

### `space-between-symbol-and-amount`

Controls whether there is a space between the symbol and the numeric amount.

Example:

- `$100`
- `$ 100`

### `space-between-amount-and-name`

Controls whether there is a space between the formatted amount and the currency name.

Example:

- `$100 coins`
- `$100coins`

### `use-grouping`

Controls whether large numbers use grouping separators.

Example:

- `1000`
- `1,000`

### `show-trailing-zeros`

Controls whether trailing decimal zeroes are shown.

Example:

- `10.00`
- `10`

## Default Example Setup

The default config ships with two example currencies:

- `coins`
- `gems`

Example:

```yml
currencies:
  default: coins
  definitions:
    coins:
      singular: coin
      plural: coins
      symbol: $
      fractional-digits: 2
      starting-balance: 100.00
      format:
        symbol-first: true
        space-between-symbol-and-amount: false
        space-between-amount-and-name: true
        use-grouping: true
        show-trailing-zeros: true
    gems:
      singular: gem
      plural: gems
      symbol: ◆
      fractional-digits: 0
      starting-balance: 0
      format:
        symbol-first: false
        space-between-symbol-and-amount: true
        space-between-amount-and-name: true
        use-grouping: true
        show-trailing-zeros: false
```

## Using Currencies in Commands

Many Coffers commands can accept a currency argument.

Examples:

- `/coffers balance Kai coins`
- `/coffers pay Kai 50 gems`
- `/coffers set Kai 500 coins`

Legacy examples:

- `/cofferslegacy balance Kai coins`
- `/cofferslegacy pay Kai 50 gems`
- `/cofferslegacy set Kai 500 coins`

If no currency is specified, Coffers uses the default currency.

## Multi-Currency and Vault

Coffers supports multiple currencies internally.

However, Vault compatibility is primarily centered on the default currency.

That means:

- direct Coffers usage can work with multiple configured currencies
- Vault-aware third-party plugins generally interact through the default Coffers currency
- advanced multi-currency behavior is better handled through Coffers-native integrations

## Best Practices

Recommended currency setup habits:

- keep IDs simple and lowercase
- make sure the default currency is defined
- use `0` fractional digits for whole-number currencies
- use readable symbols and names
- keep formatting rules consistent with the style of your server

## Common Mistakes

Avoid these common currency problems:

- setting a default currency that is not defined
- using confusing or inconsistent currency IDs
- mixing decimal and whole-number expectations without checking fractional digits
- assuming Vault-aware plugins will automatically support every Coffers currency

## Summary

Coffers currencies are designed to be flexible, configurable, and more expressive than the old single-currency approach common in older economy systems.

You can control:

- what currencies exist
- how they display
- how many decimals they use
- what starting balances they assign
- how commands and formatted output present them

## Next Step

Continue to:

- [Developer API](Developer-API)
- [Legacy Line](Legacy-Line)
- [FAQ](FAQ)
