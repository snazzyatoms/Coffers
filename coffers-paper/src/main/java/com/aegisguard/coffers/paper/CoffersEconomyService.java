package com.aegisguard.coffers.paper;

import com.aegisguard.coffers.api.AccountSnapshot;
import com.aegisguard.coffers.api.CoffersEconomy;
import com.aegisguard.coffers.api.CurrencyDefinition;
import com.aegisguard.coffers.api.CurrencyFormat;
import com.aegisguard.coffers.api.LedgerEntry;
import com.aegisguard.coffers.api.TransactionActor;
import com.aegisguard.coffers.api.TransactionFailure;
import com.aegisguard.coffers.api.TransactionKind;
import com.aegisguard.coffers.api.TransactionResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

final class CoffersEconomyService implements CoffersEconomy {

    private final Map<UUID, Map<String, BigDecimal>> balances = new ConcurrentHashMap<>();
    private final Map<UUID, List<LedgerEntry>> history = new ConcurrentHashMap<>();
    private final Map<String, CurrencyDefinition> currencies;
    private final String defaultCurrencyId;
    private final EconomyStorage storage;
    private final int historyLimit;
    private final Logger logger;
    private final JavaPlugin plugin;

    CoffersEconomyService(
            final Collection<CurrencyDefinition> currencies,
            final String defaultCurrencyId,
            final EconomyStorage storage,
            final int historyLimit,
            final StorageSnapshot snapshot,
            final Logger logger,
            final JavaPlugin plugin
    ) {
        this.currencies = new LinkedHashMap<>();
        for (final CurrencyDefinition currency : currencies) {
            this.currencies.put(currency.id().toLowerCase(Locale.ROOT), currency);
        }
        if (this.currencies.isEmpty()) {
            throw new IllegalArgumentException("At least one currency must be configured.");
        }
        this.defaultCurrencyId = defaultCurrencyId.toLowerCase(Locale.ROOT);
        if (!this.currencies.containsKey(this.defaultCurrencyId)) {
            throw new IllegalArgumentException("Default currency is not registered: " + defaultCurrencyId);
        }
        this.storage = storage;
        this.historyLimit = historyLimit;
        this.logger = logger;
        this.plugin = plugin;
        this.balances.putAll(snapshot.balances());
        this.history.putAll(snapshot.history());
    }

    @Override
    public String defaultCurrencyId() {
        return this.defaultCurrencyId;
    }

    @Override
    public Collection<CurrencyDefinition> currencies() {
        return Collections.unmodifiableCollection(this.currencies.values());
    }

    @Override
    public Optional<CurrencyDefinition> currency(final String currencyId) {
        if (currencyId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(this.currencies.get(currencyId.toLowerCase(Locale.ROOT)));
    }

    @Override
    public boolean hasAccount(final UUID accountId) {
        return this.balances.containsKey(accountId);
    }

    @Override
    public synchronized void createAccount(final UUID accountId) {
        final Map<String, BigDecimal> accountBalances = this.balances.computeIfAbsent(accountId, ignored -> new ConcurrentHashMap<>());
        for (final CurrencyDefinition currency : this.currencies.values()) {
            accountBalances.putIfAbsent(currency.id(), normalize(currency.id(), currency.startingBalance()));
        }
        persistAccount(accountId);
    }

    @Override
    public synchronized AccountSnapshot account(final UUID accountId, final String currencyId) {
        return new AccountSnapshot(accountId, normalizedCurrencyId(currencyId), getBalance(accountId, currencyId));
    }

    @Override
    public synchronized BigDecimal getBalance(final UUID accountId, final String currencyId) {
        final String normalizedCurrencyId = normalizedCurrencyId(currencyId);
        createAccount(accountId);
        return this.balances.get(accountId).get(normalizedCurrencyId);
    }

    @Override
    public synchronized TransactionResult deposit(
            final UUID accountId,
            final String currencyId,
            final BigDecimal amount,
            final TransactionActor actor,
            final String reason
    ) {
        final String normalizedCurrencyId = normalizedCurrencyId(currencyId);
        final BigDecimal normalizedAmount = validateAmount(normalizedCurrencyId, amount);
        if (normalizedAmount == null) {
            return invalidAmount(accountId, normalizedCurrencyId, amount);
        }

        createAccount(accountId);
        final BigDecimal previousBalance = getBalance(accountId, normalizedCurrencyId);
        final BigDecimal nextBalance = previousBalance.add(normalizedAmount);
        this.balances.get(accountId).put(normalizedCurrencyId, nextBalance);

        final LedgerEntry entry = recordEntry(
                accountId,
                null,
                normalizedCurrencyId,
                TransactionKind.DEPOSIT,
                normalizedAmount,
                previousBalance,
                nextBalance,
                actor,
                reason,
                UUID.randomUUID(),
                null
        );
        persistAccount(accountId);
        persistHistory(accountId);
        final TransactionResult result = TransactionResult.success(normalizedCurrencyId, normalizedAmount, nextBalance, reason, entry);
        notifyTransaction(TransactionKind.DEPOSIT, accountId, null, normalizedCurrencyId, result, actor);
        return result;
    }

    @Override
    public synchronized TransactionResult withdraw(
            final UUID accountId,
            final String currencyId,
            final BigDecimal amount,
            final TransactionActor actor,
            final String reason
    ) {
        final String normalizedCurrencyId = normalizedCurrencyId(currencyId);
        final BigDecimal normalizedAmount = validateAmount(normalizedCurrencyId, amount);
        if (normalizedAmount == null) {
            return invalidAmount(accountId, normalizedCurrencyId, amount);
        }

        createAccount(accountId);
        final BigDecimal currentBalance = getBalance(accountId, normalizedCurrencyId);
        if (currentBalance.compareTo(normalizedAmount) < 0) {
            return TransactionResult.failure(
                    normalizedCurrencyId,
                    normalizedAmount,
                    currentBalance,
                    TransactionFailure.INSUFFICIENT_FUNDS,
                    "Insufficient funds."
            );
        }

        final BigDecimal nextBalance = currentBalance.subtract(normalizedAmount);
        this.balances.get(accountId).put(normalizedCurrencyId, nextBalance);

        final LedgerEntry entry = recordEntry(
                accountId,
                null,
                normalizedCurrencyId,
                TransactionKind.WITHDRAWAL,
                normalizedAmount,
                currentBalance,
                nextBalance,
                actor,
                reason,
                UUID.randomUUID(),
                null
        );
        persistAccount(accountId);
        persistHistory(accountId);
        final TransactionResult result = TransactionResult.success(normalizedCurrencyId, normalizedAmount, nextBalance, reason, entry);
        notifyTransaction(TransactionKind.WITHDRAWAL, accountId, null, normalizedCurrencyId, result, actor);
        return result;
    }

    @Override
    public synchronized TransactionResult transfer(
            final UUID fromAccountId,
            final UUID toAccountId,
            final String currencyId,
            final BigDecimal amount,
            final TransactionActor actor,
            final String reason
    ) {
        final String normalizedCurrencyId = normalizedCurrencyId(currencyId);
        final BigDecimal normalizedAmount = validateAmount(normalizedCurrencyId, amount);
        if (normalizedAmount == null) {
            return invalidAmount(fromAccountId, normalizedCurrencyId, amount);
        }

        createAccount(fromAccountId);
        createAccount(toAccountId);
        final BigDecimal fromBalance = getBalance(fromAccountId, normalizedCurrencyId);
        if (fromBalance.compareTo(normalizedAmount) < 0) {
            return TransactionResult.failure(
                    normalizedCurrencyId,
                    normalizedAmount,
                    fromBalance,
                    TransactionFailure.INSUFFICIENT_FUNDS,
                    "Insufficient funds."
            );
        }

        final UUID referenceId = UUID.randomUUID();
        final BigDecimal nextFromBalance = fromBalance.subtract(normalizedAmount);
        final BigDecimal nextToBalance = getBalance(toAccountId, normalizedCurrencyId).add(normalizedAmount);

        this.balances.get(fromAccountId).put(normalizedCurrencyId, nextFromBalance);
        this.balances.get(toAccountId).put(normalizedCurrencyId, nextToBalance);

        final LedgerEntry outEntry = recordEntry(
                fromAccountId,
                toAccountId,
                normalizedCurrencyId,
                TransactionKind.TRANSFER_OUT,
                normalizedAmount,
                fromBalance,
                nextFromBalance,
                actor,
                reason,
                referenceId,
                null
        );
        final LedgerEntry inEntry = recordEntry(
                toAccountId,
                fromAccountId,
                normalizedCurrencyId,
                TransactionKind.TRANSFER_IN,
                normalizedAmount,
                getBalance(toAccountId, normalizedCurrencyId).subtract(normalizedAmount),
                nextToBalance,
                actor,
                reason,
                referenceId,
                null
        );

        persistAccount(fromAccountId);
        persistAccount(toAccountId);
        persistHistory(fromAccountId);
        persistHistory(toAccountId);
        final TransactionResult result = TransactionResult.success(normalizedCurrencyId, normalizedAmount, nextFromBalance, reason, outEntry);
        notifyTransaction(TransactionKind.TRANSFER_OUT, fromAccountId, toAccountId, normalizedCurrencyId, result, actor);
        notifyTransaction(
                TransactionKind.TRANSFER_IN,
                toAccountId,
                fromAccountId,
                normalizedCurrencyId,
                TransactionResult.success(normalizedCurrencyId, normalizedAmount, nextToBalance, reason, inEntry),
                actor
        );
        return result;
    }

    @Override
    public synchronized TransactionResult setBalance(
            final UUID accountId,
            final String currencyId,
            final BigDecimal amount,
            final TransactionActor actor,
            final String reason
    ) {
        final String normalizedCurrencyId = normalizedCurrencyId(currencyId);
        final BigDecimal normalizedAmount = validateAmount(normalizedCurrencyId, amount);
        if (normalizedAmount == null) {
            return invalidAmount(accountId, normalizedCurrencyId, amount);
        }

        createAccount(accountId);
        final BigDecimal previousBalance = getBalance(accountId, normalizedCurrencyId);
        this.balances.get(accountId).put(normalizedCurrencyId, normalizedAmount);
        final LedgerEntry entry = recordEntry(
                accountId,
                null,
                normalizedCurrencyId,
                TransactionKind.SET,
                BigDecimal.ZERO.setScale(currency(normalizedCurrencyId).orElseThrow().fractionalDigits(), RoundingMode.HALF_UP),
                previousBalance,
                normalizedAmount,
                actor,
                reason,
                UUID.randomUUID(),
                null
        );
        persistAccount(accountId);
        persistHistory(accountId);
        final TransactionResult result = TransactionResult.success(normalizedCurrencyId, BigDecimal.ZERO, normalizedAmount, reason, entry);
        notifyTransaction(TransactionKind.SET, accountId, null, normalizedCurrencyId, result, actor);
        return result;
    }

    @Override
    public synchronized List<LedgerEntry> recentTransactions(final UUID accountId, final int limit) {
        return transactionHistory(accountId, 0, limit);
    }

    @Override
    public synchronized List<LedgerEntry> transactionHistory(final UUID accountId, final int offset, final int limit) {
        return filteredTransactionHistory(accountId, offset, limit, null, null);
    }

    synchronized List<LedgerEntry> filteredTransactionHistory(
            final UUID accountId,
            final int offset,
            final int limit,
            final String currencyId,
            final TransactionKind kind
    ) {
        final String normalizedCurrencyId = currencyId == null || currencyId.isBlank() ? null : normalizedCurrencyId(currencyId);
        final List<LedgerEntry> entries = new ArrayList<>(this.history.getOrDefault(accountId, List.of()));
        entries.sort(Comparator.comparingLong(LedgerEntry::createdAtEpochMilli).reversed());
        return entries.stream()
                .filter(entry -> normalizedCurrencyId == null || normalizedCurrencyId.equals(entry.currencyId()))
                .filter(entry -> kind == null || kind == entry.kind())
                .skip(Math.max(offset, 0))
                .limit(Math.max(limit, 0))
                .toList();
    }

    @Override
    public synchronized List<AccountSnapshot> topAccounts(final String currencyId, final int limit) {
        return topAccounts(currencyId, limit, accountId -> true);
    }

    synchronized List<AccountSnapshot> topAccounts(final String currencyId, final int limit, final Predicate<UUID> includeAccount) {
        final String normalizedCurrencyId = normalizedCurrencyId(currencyId);
        return this.balances.entrySet().stream()
                .filter(entry -> includeAccount.test(entry.getKey()))
                .map(entry -> new AccountSnapshot(
                        entry.getKey(),
                        normalizedCurrencyId,
                        entry.getValue().getOrDefault(normalizedCurrencyId, startingBalance(normalizedCurrencyId))
                ))
                .sorted(Comparator.comparing(AccountSnapshot::balance).reversed())
                .limit(Math.max(limit, 0))
                .toList();
    }

    synchronized LedgerEntry findEntry(final UUID entryId) {
        for (final List<LedgerEntry> entries : this.history.values()) {
            for (final LedgerEntry entry : entries) {
                if (entry.entryId().equals(entryId)) {
                    return entry;
                }
            }
        }
        return null;
    }

    synchronized TransactionResult rollback(final UUID entryId, final TransactionActor actor, final String reason) {
        final LedgerEntry target = findEntry(entryId);
        if (target == null) {
            return TransactionResult.failure(this.defaultCurrencyId, BigDecimal.ZERO, BigDecimal.ZERO, TransactionFailure.NOT_FOUND, "No Coffers ledger entry was found for that id.");
        }
        if (hasReversalForReference(target.referenceId())) {
            return TransactionResult.failure(target.currencyId(), target.amount(), target.resultingBalance(), TransactionFailure.ROLLBACK_UNAVAILABLE, "That Coffers transaction has already been rolled back.");
        }

        final String rollbackReason = (reason == null || reason.isBlank())
                ? "Rollback of transaction " + target.entryId()
                : reason;

        return switch (target.kind()) {
            case DEPOSIT -> rollbackWithdrawal(target, actor, rollbackReason);
            case WITHDRAWAL -> rollbackDeposit(target, actor, rollbackReason);
            case TRANSFER_IN, TRANSFER_OUT -> rollbackTransfer(target, actor, rollbackReason);
            case SET -> rollbackSet(target, actor, rollbackReason);
        };
    }

    synchronized void purgeAccount(final UUID accountId) {
        this.balances.remove(accountId);
        this.history.remove(accountId);
        persistAccountSnapshot(accountId, Map.of());
        persistHistorySnapshot(accountId, List.of());
    }

    @Override
    public String format(final String currencyId, final BigDecimal amount) {
        final CurrencyDefinition currency = currency(currencyId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown currency: " + currencyId));
        final BigDecimal normalized = normalize(currency.id(), amount);
        final CurrencyFormat format = currency.format();

        final DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.US);
        final StringBuilder pattern = new StringBuilder(format.useGrouping() ? "#,##0" : "0");
        if (currency.fractionalDigits() > 0) {
            pattern.append('.');
            for (int index = 0; index < currency.fractionalDigits(); index++) {
                pattern.append(format.showTrailingZeros() ? '0' : '#');
            }
        }

        final DecimalFormat decimalFormat = new DecimalFormat(pattern.toString(), symbols);
        decimalFormat.setRoundingMode(RoundingMode.HALF_UP);

        final String formattedAmount = decimalFormat.format(normalized);
        final String unit = normalized.compareTo(BigDecimal.ONE.setScale(currency.fractionalDigits(), RoundingMode.HALF_UP)) == 0
                ? currency.singularName()
                : currency.pluralName();

        final String amountWithSymbol;
        if (currency.symbol() == null || currency.symbol().isBlank()) {
            amountWithSymbol = formattedAmount;
        } else if (format.symbolFirst()) {
            amountWithSymbol = currency.symbol() + (format.spaceBetweenSymbolAndAmount() ? " " : "") + formattedAmount;
        } else {
            amountWithSymbol = formattedAmount + (format.spaceBetweenSymbolAndAmount() ? " " : "") + currency.symbol();
        }

        return amountWithSymbol + (format.spaceBetweenAmountAndName() ? " " : "") + unit;
    }

    void close() {
        try {
            this.storage.close();
        } catch (final Exception exception) {
            this.logger.warning("Failed to close Coffers storage cleanly: " + exception.getMessage());
        }
    }

    synchronized StorageSnapshot snapshot() {
        final Map<UUID, Map<String, BigDecimal>> balanceCopy = new LinkedHashMap<>();
        for (final Map.Entry<UUID, Map<String, BigDecimal>> entry : this.balances.entrySet()) {
            balanceCopy.put(entry.getKey(), new LinkedHashMap<>(entry.getValue()));
        }

        final Map<UUID, List<LedgerEntry>> historyCopy = new LinkedHashMap<>();
        for (final Map.Entry<UUID, List<LedgerEntry>> entry : this.history.entrySet()) {
            historyCopy.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return new StorageSnapshot(balanceCopy, historyCopy);
    }

    synchronized void replaceSnapshot(final StorageSnapshot snapshot) {
        final Set<UUID> previousAccountIds = new LinkedHashSet<>();
        previousAccountIds.addAll(this.balances.keySet());
        previousAccountIds.addAll(this.history.keySet());

        this.balances.clear();
        this.history.clear();

        for (final Map.Entry<UUID, Map<String, BigDecimal>> entry : snapshot.balances().entrySet()) {
            final Map<String, BigDecimal> accountBalances = new ConcurrentHashMap<>();
            for (final CurrencyDefinition currency : this.currencies.values()) {
                final BigDecimal importedBalance = entry.getValue().get(currency.id());
                accountBalances.put(
                        currency.id(),
                        normalize(currency.id(), importedBalance == null ? currency.startingBalance() : importedBalance)
                );
            }
            this.balances.put(entry.getKey(), accountBalances);
            persistAccount(entry.getKey());
        }

        for (final Map.Entry<UUID, List<LedgerEntry>> entry : snapshot.history().entrySet()) {
            this.history.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            persistHistory(entry.getKey());
        }

        final Set<UUID> importedAccountIds = new LinkedHashSet<>();
        importedAccountIds.addAll(snapshot.balances().keySet());
        importedAccountIds.addAll(snapshot.history().keySet());
        previousAccountIds.removeAll(importedAccountIds);

        for (final UUID removedAccountId : previousAccountIds) {
            persistAccountSnapshot(removedAccountId, Map.of());
            persistHistorySnapshot(removedAccountId, List.of());
        }
    }

    synchronized boolean restoreAccountFromSnapshot(final UUID accountId, final StorageSnapshot snapshot) {
        final Map<String, BigDecimal> importedBalances = snapshot.balances().get(accountId);
        final List<LedgerEntry> importedHistory = snapshot.history().get(accountId);
        if (importedBalances == null && importedHistory == null) {
            return false;
        }

        final Map<String, BigDecimal> accountBalances = new ConcurrentHashMap<>();
        for (final CurrencyDefinition currency : this.currencies.values()) {
            final BigDecimal importedBalance = importedBalances == null ? null : importedBalances.get(currency.id());
            accountBalances.put(
                    currency.id(),
                    normalize(currency.id(), importedBalance == null ? currency.startingBalance() : importedBalance)
            );
        }

        this.balances.put(accountId, accountBalances);
        this.history.put(accountId, new ArrayList<>(importedHistory == null ? List.of() : importedHistory));
        persistAccount(accountId);
        persistHistory(accountId);
        return true;
    }

    private LedgerEntry recordEntry(
            final UUID accountId,
            final UUID counterpartyAccountId,
            final String currencyId,
            final TransactionKind kind,
            final BigDecimal amount,
            final BigDecimal previousBalance,
            final BigDecimal resultingBalance,
            final TransactionActor actor,
            final String reason,
            final UUID referenceId,
            final UUID reversalOfReferenceId
    ) {
        final TransactionActor normalizedActor = normalizeActor(actor);
        final LedgerEntry entry = new LedgerEntry(
                UUID.randomUUID(),
                referenceId,
                accountId,
                counterpartyAccountId,
                currencyId,
                kind,
                amount,
                previousBalance,
                resultingBalance,
                normalizedActor,
                reason,
                reversalOfReferenceId,
                System.currentTimeMillis()
        );

        final List<LedgerEntry> accountHistory = this.history.computeIfAbsent(accountId, ignored -> new ArrayList<>());
        accountHistory.add(entry);
        while (accountHistory.size() > this.historyLimit) {
            accountHistory.remove(0);
        }
        return entry;
    }

    private BigDecimal validateAmount(final String currencyId, final BigDecimal amount) {
        if (amount == null || amount.signum() < 0) {
            return null;
        }
        return normalize(currencyId, amount);
    }

    private TransactionResult rollbackWithdrawal(final LedgerEntry target, final TransactionActor actor, final String reason) {
        final BigDecimal amount = normalize(target.currencyId(), target.amount());
        final BigDecimal currentBalance = getBalance(target.accountId(), target.currencyId());
        if (currentBalance.compareTo(amount) < 0) {
            return TransactionResult.failure(target.currencyId(), amount, currentBalance, TransactionFailure.INSUFFICIENT_FUNDS, "Not enough balance remains to roll back that deposit.");
        }

        this.balances.get(target.accountId()).put(target.currencyId(), currentBalance.subtract(amount));
        final LedgerEntry entry = recordEntry(
                target.accountId(),
                null,
                target.currencyId(),
                TransactionKind.WITHDRAWAL,
                amount,
                currentBalance,
                currentBalance.subtract(amount),
                actor,
                reason,
                UUID.randomUUID(),
                target.referenceId()
        );
        persistAccount(target.accountId());
        persistHistory(target.accountId());
        final TransactionResult result = TransactionResult.success(target.currencyId(), amount, currentBalance.subtract(amount), reason, entry);
        notifyTransaction(TransactionKind.WITHDRAWAL, target.accountId(), null, target.currencyId(), result, actor);
        return result;
    }

    private TransactionResult rollbackDeposit(final LedgerEntry target, final TransactionActor actor, final String reason) {
        final BigDecimal amount = normalize(target.currencyId(), target.amount());
        final BigDecimal currentBalance = getBalance(target.accountId(), target.currencyId());
        this.balances.get(target.accountId()).put(target.currencyId(), currentBalance.add(amount));
        final LedgerEntry entry = recordEntry(
                target.accountId(),
                null,
                target.currencyId(),
                TransactionKind.DEPOSIT,
                amount,
                currentBalance,
                currentBalance.add(amount),
                actor,
                reason,
                UUID.randomUUID(),
                target.referenceId()
        );
        persistAccount(target.accountId());
        persistHistory(target.accountId());
        final TransactionResult result = TransactionResult.success(target.currencyId(), amount, currentBalance.add(amount), reason, entry);
        notifyTransaction(TransactionKind.DEPOSIT, target.accountId(), null, target.currencyId(), result, actor);
        return result;
    }

    private TransactionResult rollbackTransfer(final LedgerEntry target, final TransactionActor actor, final String reason) {
        if (target.counterpartyAccountId() == null) {
            return TransactionResult.failure(target.currencyId(), target.amount(), target.resultingBalance(), TransactionFailure.ROLLBACK_UNAVAILABLE, "That transfer cannot be rolled back because the other account is missing.");
        }
        final UUID fromAccountId = target.accountId();
        final UUID toAccountId = target.counterpartyAccountId();
        final BigDecimal amount = normalize(target.currencyId(), target.amount());
        final BigDecimal fromBalance = getBalance(fromAccountId, target.currencyId());
        if (fromBalance.compareTo(amount) < 0) {
            return TransactionResult.failure(target.currencyId(), amount, fromBalance, TransactionFailure.INSUFFICIENT_FUNDS, "The receiving account no longer has enough balance to roll back this transfer.");
        }

        final BigDecimal toBalance = getBalance(toAccountId, target.currencyId());
        final UUID referenceId = UUID.randomUUID();
        final BigDecimal nextFromBalance = fromBalance.subtract(amount);
        final BigDecimal nextToBalance = toBalance.add(amount);
        this.balances.get(fromAccountId).put(target.currencyId(), nextFromBalance);
        this.balances.get(toAccountId).put(target.currencyId(), nextToBalance);

        final LedgerEntry outEntry = recordEntry(
                fromAccountId,
                toAccountId,
                target.currencyId(),
                TransactionKind.TRANSFER_OUT,
                amount,
                fromBalance,
                nextFromBalance,
                actor,
                reason,
                referenceId,
                target.referenceId()
        );
        final LedgerEntry inEntry = recordEntry(
                toAccountId,
                fromAccountId,
                target.currencyId(),
                TransactionKind.TRANSFER_IN,
                amount,
                toBalance,
                nextToBalance,
                actor,
                reason,
                referenceId,
                target.referenceId()
        );

        persistAccount(fromAccountId);
        persistAccount(toAccountId);
        persistHistory(fromAccountId);
        persistHistory(toAccountId);
        final TransactionResult result = TransactionResult.success(target.currencyId(), amount, nextFromBalance, reason, outEntry);
        notifyTransaction(TransactionKind.TRANSFER_OUT, fromAccountId, toAccountId, target.currencyId(), result, actor);
        notifyTransaction(
                TransactionKind.TRANSFER_IN,
                toAccountId,
                fromAccountId,
                target.currencyId(),
                TransactionResult.success(target.currencyId(), amount, nextToBalance, reason, inEntry),
                actor
        );
        return result;
    }

    private TransactionResult rollbackSet(final LedgerEntry target, final TransactionActor actor, final String reason) {
        this.balances.get(target.accountId()).put(target.currencyId(), normalize(target.currencyId(), target.previousBalance()));
        final BigDecimal zero = BigDecimal.ZERO.setScale(currency(target.currencyId()).orElseThrow().fractionalDigits(), RoundingMode.HALF_UP);
        final LedgerEntry entry = recordEntry(
                target.accountId(),
                null,
                target.currencyId(),
                TransactionKind.SET,
                zero,
                target.resultingBalance(),
                normalize(target.currencyId(), target.previousBalance()),
                actor,
                reason,
                UUID.randomUUID(),
                target.referenceId()
        );
        persistAccount(target.accountId());
        persistHistory(target.accountId());
        final TransactionResult result = TransactionResult.success(target.currencyId(), zero, normalize(target.currencyId(), target.previousBalance()), reason, entry);
        notifyTransaction(TransactionKind.SET, target.accountId(), null, target.currencyId(), result, actor);
        return result;
    }

    private boolean hasReversalForReference(final UUID referenceId) {
        for (final List<LedgerEntry> entries : this.history.values()) {
            for (final LedgerEntry entry : entries) {
                if (referenceId.equals(entry.reversalOfReferenceId())) {
                    return true;
                }
            }
        }
        return false;
    }

    private TransactionResult invalidAmount(final UUID accountId, final String currencyId, final BigDecimal attemptedAmount) {
        return TransactionResult.failure(
                currencyId,
                attemptedAmount == null ? BigDecimal.ZERO : normalize(currencyId, attemptedAmount),
                getBalance(accountId, currencyId),
                TransactionFailure.INVALID_AMOUNT,
                "Amount must be zero or greater."
        );
    }

    private BigDecimal normalize(final String currencyId, final BigDecimal amount) {
        final CurrencyDefinition currency = currency(currencyId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown currency: " + currencyId));
        return amount.setScale(currency.fractionalDigits(), RoundingMode.HALF_UP);
    }

    private BigDecimal startingBalance(final String currencyId) {
        return currency(currencyId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown currency: " + currencyId))
                .startingBalance();
    }

    private String normalizedCurrencyId(final String currencyId) {
        final String normalized = currencyId == null ? this.defaultCurrencyId : currencyId.toLowerCase(Locale.ROOT);
        if (!this.currencies.containsKey(normalized)) {
            throw new IllegalArgumentException("Unknown currency: " + currencyId);
        }
        return normalized;
    }

    private void persistAccount(final UUID accountId) {
        persistAccountSnapshot(accountId, this.balances.getOrDefault(accountId, Map.of()));
    }

    private void persistAccountSnapshot(final UUID accountId, final Map<String, BigDecimal> snapshotBalances) {
        try {
            this.storage.saveAccount(accountId, new LinkedHashMap<>(snapshotBalances));
        } catch (final Exception exception) {
            throw new IllegalStateException("Failed to persist Coffers account " + accountId, exception);
        }
    }

    private void persistHistory(final UUID accountId) {
        persistHistorySnapshot(accountId, this.history.getOrDefault(accountId, List.of()));
    }

    private void persistHistorySnapshot(final UUID accountId, final List<LedgerEntry> snapshotHistory) {
        try {
            this.storage.saveHistory(accountId, List.copyOf(snapshotHistory));
        } catch (final Exception exception) {
            throw new IllegalStateException("Failed to persist Coffers history for " + accountId, exception);
        }
    }

    private void notifyTransaction(
            final TransactionKind kind,
            final UUID accountId,
            final UUID counterpartyAccountId,
            final String currencyId,
            final TransactionResult result,
            final TransactionActor actor
    ) {
        if (this.plugin == null || !this.plugin.isEnabled()) {
            return;
        }

        Bukkit.getPluginManager().callEvent(new CoffersTransactionEvent(
                accountId,
                counterpartyAccountId,
                kind,
                currencyId,
                result,
                normalizeActor(actor)
        ));
    }

    private TransactionActor normalizeActor(final TransactionActor actor) {
        return actor != null ? actor : TransactionActor.system("coffers-runtime");
    }
}
