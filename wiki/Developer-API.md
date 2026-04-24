# Developer API

This page introduces the Coffers developer API and explains how plugin authors can integrate directly with Coffers.

Starting with `v0.2`, this API should be treated as the preferred integration path for new Coffers-aware plugins.

## Overview

Coffers includes a separate developer jar:

- `Coffers-API.jar`

This jar is intended for plugin developers who want direct access to Coffers types and economy behavior.

It is not intended to be installed as the main server economy plugin by itself.

## Why Use the Coffers API?

Direct Coffers integration gives plugin developers a richer foundation than relying only on Vault-style balance access.

Benefits include:

- direct currency definitions
- transaction results
- ledger history access
- transaction actor metadata
- richer failure reporting
- a cleaner economy-oriented API surface

Vault compatibility is still useful for legacy support, but the Coffers API is the better long-term integration target for Coffers-aware plugins.

## Main Interface

The main API entry point is:

- `CoffersEconomy`

This interface provides methods for:

- discovering currencies
- checking balances
- creating accounts
- depositing funds
- withdrawing funds
- transferring funds
- setting balances
- reading recent transactions
- formatting balances for display

## Core API Types

The current API includes the following main types:

- `CoffersEconomy`
- `CurrencyDefinition`
- `CurrencyFormat`
- `AccountSnapshot`
- `TransactionResult`
- `TransactionFailure`
- `TransactionKind`
- `TransactionActor`
- `TransactionActorType`
- `LedgerEntry`

## `CoffersEconomy`

The `CoffersEconomy` interface is the main service contract.

Key capabilities include:

- `defaultCurrencyId()`
- `currencies()`
- `currency(String currencyId)`
- `hasAccount(UUID accountId)`
- `createAccount(UUID accountId)`
- `account(UUID accountId, String currencyId)`
- `getBalance(UUID accountId, String currencyId)`
- `deposit(...)`
- `withdraw(...)`
- `transfer(...)`
- `setBalance(...)`
- `recentTransactions(UUID accountId, int limit)`
- `transactionHistory(UUID accountId, int offset, int limit)`
- `topAccounts(String currencyId, int limit)`
- `format(String currencyId, BigDecimal amount)`

The interface also includes convenience defaults for:

- default currency name access
- default currency symbol access
- default fractional digit access
- simplified balance, deposit, withdraw, transfer, and set-balance operations

## `CurrencyDefinition`

`CurrencyDefinition` describes a configured currency.

It includes:

- currency ID
- singular name
- plural name
- symbol
- fractional digits
- starting balance
- formatting rules

This allows plugins to understand not just a balance number, but how a currency is actually defined and displayed.

## `CurrencyFormat`

`CurrencyFormat` controls how balances are displayed.

It includes options for:

- symbol placement
- spacing
- grouping separators
- trailing zero behavior

This helps other plugins keep their Coffers-related displays consistent with the server's configured economy presentation.

## `AccountSnapshot`

`AccountSnapshot` provides a lightweight account state view.

It includes:

- account UUID
- currency ID
- current balance

This is useful when a plugin wants a simple account read without requesting a full transaction flow.

## `TransactionResult`

`TransactionResult` is returned by balance-changing operations.

It includes:

- whether the action succeeded
- currency ID
- amount involved
- resulting balance
- failure type
- message
- related ledger entry when available

This is much richer than a simple true or false response.

## `TransactionFailure`

`TransactionFailure` describes why a transaction failed.

Current values include:

- `NONE`
- `INVALID_AMOUNT`
- `INSUFFICIENT_FUNDS`
- `NOT_FOUND`
- `ROLLBACK_UNAVAILABLE`

This allows plugin developers to react more intelligently to failed transactions.

## `TransactionKind`

`TransactionKind` describes the type of ledger action.

Current values include:

- `DEPOSIT`
- `WITHDRAWAL`
- `TRANSFER_IN`
- `TRANSFER_OUT`
- `SET`

This is especially useful for plugins that want to inspect or present transaction history.

## `TransactionActor`

`TransactionActor` records who or what caused a transaction.

It includes:

- actor type
- actor UUID when available
- actor name
- source string

Factory helpers currently include:

- system
- console
- player
- plugin
- migration
- vault

This gives Coffers integrations better audit detail than a balance-only API.

## `TransactionActorType`

`TransactionActorType` identifies the source category behind a transaction.

This allows plugin authors to distinguish, for example:

- player-driven changes
- console-driven changes
- migration actions
- Vault-triggered operations

## `LedgerEntry`

`LedgerEntry` represents an individual recorded transaction entry.

It includes:

- entry ID
- reference ID
- account ID
- counterparty account ID
- currency ID
- transaction kind
- amount
- previous balance
- resulting balance
- actor metadata
- reason
- reversal reference information
- timestamp

This allows richer logging, auditing, and future plugin workflows than a simple current-balance-only model.

## Paper Events

The modern Paper line now emits:

- `CoffersTransactionEvent`

This event is fired when Coffers successfully applies a transaction in the modern line.

It includes:

- the affected account
- an optional counterparty account
- transaction kind
- currency ID
- the resulting `TransactionResult`
- actor metadata

This gives Paper-side plugins an event-based way to observe Coffers activity without polling balances.

## Typical Integration Flow

A Coffers-aware plugin will usually:

1. obtain the `CoffersEconomy` service
2. discover the currency it wants to use
3. read balances or account state
4. perform a deposit, withdrawal, transfer, or set operation
5. inspect the returned `TransactionResult`

This is a more expressive flow than simply calling Vault and getting a yes or no answer.

## Developer Examples

Practical integration examples are also documented in:

- [Developer-Examples](Developer-Examples)

## Coffers API vs Vault

Use the Coffers API when:

- you want direct Coffers-aware integration
- you need richer result objects
- you need audit or ledger data
- you care about multi-currency behavior
- you want future Coffers-native integration support

Use Vault compatibility when:

- you are supporting legacy plugin ecosystems
- you are integrating with general economy plugins rather than Coffers specifically
- you only need broad compatibility with existing Vault-based systems

## v0.2 Architectural Position

Starting in `v0.2`, the intended architecture is:

- Coffers-native features live in Coffers itself
- new integrations should prefer `CoffersEconomy`
- Vault should remain available only as a compatibility layer for older ecosystems
- richer features should be added to Coffers API rather than squeezed into older Vault-era patterns

## Important Notes

Keep in mind:

- Vault compatibility is centered on the default Coffers currency
- Coffers-native integrations are the better path for richer features
- `Coffers-API.jar` is for developers, not normal server owners
- the API is currently intended to be version-matched with the Coffers release you are targeting

## Recommended Developer Strategy

For plugin authors building directly for Coffers:

- prefer direct Coffers API integration
- use `TransactionResult` instead of assuming success
- use `CurrencyDefinition` and `CurrencyFormat` instead of hardcoding display rules
- use `LedgerEntry` and actor metadata if your plugin needs history or audit awareness

## Summary

The Coffers API is designed to provide a stronger plugin integration foundation than the older balance-only patterns common in Vault-first designs.

It gives developers:

- richer data
- clearer transaction outcomes
- better metadata
- stronger alignment with Coffers-native features

## Next Step

Continue to:

- [Legacy Line](Legacy-Line)
- [Standalone-First Direction](Standalone-First)
- [FAQ](FAQ)
- [Home](Home)
