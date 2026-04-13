package com.aegisguard.coffers.legacy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

final class CoffersLegacyEconomyService {

    private final Map<UUID, Map<String, BigDecimal>> balances = new ConcurrentHashMap<UUID, Map<String, BigDecimal>>();
    private final Map<UUID, List<LegacyLedgerEntry>> history = new ConcurrentHashMap<UUID, List<LegacyLedgerEntry>>();
    private final Map<String, LegacyCurrencyDefinition> currencies = new LinkedHashMap<String, LegacyCurrencyDefinition>();
    private final String defaultCurrencyId;
    private final LegacyEconomyStorage storage;
    private final int historyLimit;
    private final Logger logger;

    CoffersLegacyEconomyService(
            final Collection<LegacyCurrencyDefinition> currencyDefinitions,
            final String defaultCurrencyId,
            final LegacyEconomyStorage storage,
            final int historyLimit,
            final LegacyStorageSnapshot snapshot,
            final Logger logger
    ) {
        for (LegacyCurrencyDefinition currency : currencyDefinitions) {
            this.currencies.put(currency.getId().toLowerCase(Locale.ROOT), currency);
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
        this.balances.putAll(snapshot.getBalances());
        this.history.putAll(snapshot.getHistory());
    }

    String getDefaultCurrencyId() {
        return this.defaultCurrencyId;
    }

    Collection<LegacyCurrencyDefinition> currencies() {
        return Collections.unmodifiableCollection(this.currencies.values());
    }

    LegacyCurrencyDefinition currency(final String currencyId) {
        String normalized = currencyId == null ? this.defaultCurrencyId : currencyId.toLowerCase(Locale.ROOT);
        LegacyCurrencyDefinition currency = this.currencies.get(normalized);
        if (currency == null) {
            throw new IllegalArgumentException("Unknown currency: " + currencyId);
        }
        return currency;
    }

    boolean hasAccount(final UUID accountId) {
        return this.balances.containsKey(accountId);
    }

    synchronized void createAccount(final UUID accountId) {
        Map<String, BigDecimal> accountBalances = this.balances.get(accountId);
        if (accountBalances == null) {
            accountBalances = new ConcurrentHashMap<String, BigDecimal>();
            this.balances.put(accountId, accountBalances);
        }

        for (LegacyCurrencyDefinition currency : this.currencies.values()) {
            if (!accountBalances.containsKey(currency.getId())) {
                accountBalances.put(currency.getId(), normalize(currency.getId(), currency.getStartingBalance()));
            }
        }
        persistAccount(accountId);
    }

    synchronized LegacyAccountSnapshot account(final UUID accountId, final String currencyId) {
        return new LegacyAccountSnapshot(accountId, normalizedCurrencyId(currencyId), getBalance(accountId, currencyId));
    }

    synchronized BigDecimal getBalance(final UUID accountId, final String currencyId) {
        String normalizedCurrencyId = normalizedCurrencyId(currencyId);
        createAccount(accountId);
        return this.balances.get(accountId).get(normalizedCurrencyId);
    }

    synchronized LegacyTransactionResult deposit(
            final UUID accountId,
            final String currencyId,
            final BigDecimal amount,
            final LegacyTransactionActor actor,
            final String reason
    ) {
        String normalizedCurrencyId = normalizedCurrencyId(currencyId);
        BigDecimal normalizedAmount = validateAmount(normalizedCurrencyId, amount);
        if (normalizedAmount == null) {
            return invalidAmount(accountId, normalizedCurrencyId, amount);
        }

        createAccount(accountId);
        BigDecimal previousBalance = getBalance(accountId, normalizedCurrencyId);
        BigDecimal nextBalance = previousBalance.add(normalizedAmount);
        this.balances.get(accountId).put(normalizedCurrencyId, nextBalance);
        LegacyLedgerEntry entry = recordEntry(accountId, null, normalizedCurrencyId, LegacyTransactionKind.DEPOSIT, normalizedAmount, previousBalance, nextBalance, actor, reason, UUID.randomUUID(), null);
        persistAccount(accountId);
        persistHistory(accountId);
        return LegacyTransactionResult.success(normalizedCurrencyId, normalizedAmount, nextBalance, reason, entry);
    }

    synchronized LegacyTransactionResult withdraw(
            final UUID accountId,
            final String currencyId,
            final BigDecimal amount,
            final LegacyTransactionActor actor,
            final String reason
    ) {
        String normalizedCurrencyId = normalizedCurrencyId(currencyId);
        BigDecimal normalizedAmount = validateAmount(normalizedCurrencyId, amount);
        if (normalizedAmount == null) {
            return invalidAmount(accountId, normalizedCurrencyId, amount);
        }

        createAccount(accountId);
        BigDecimal currentBalance = getBalance(accountId, normalizedCurrencyId);
        if (currentBalance.compareTo(normalizedAmount) < 0) {
            return LegacyTransactionResult.failure(normalizedCurrencyId, normalizedAmount, currentBalance, LegacyTransactionFailure.INSUFFICIENT_FUNDS, "Insufficient funds.");
        }

        BigDecimal nextBalance = currentBalance.subtract(normalizedAmount);
        this.balances.get(accountId).put(normalizedCurrencyId, nextBalance);
        LegacyLedgerEntry entry = recordEntry(accountId, null, normalizedCurrencyId, LegacyTransactionKind.WITHDRAWAL, normalizedAmount, currentBalance, nextBalance, actor, reason, UUID.randomUUID(), null);
        persistAccount(accountId);
        persistHistory(accountId);
        return LegacyTransactionResult.success(normalizedCurrencyId, normalizedAmount, nextBalance, reason, entry);
    }

    synchronized LegacyTransactionResult transfer(
            final UUID fromAccountId,
            final UUID toAccountId,
            final String currencyId,
            final BigDecimal amount,
            final LegacyTransactionActor actor,
            final String reason
    ) {
        String normalizedCurrencyId = normalizedCurrencyId(currencyId);
        BigDecimal normalizedAmount = validateAmount(normalizedCurrencyId, amount);
        if (normalizedAmount == null) {
            return invalidAmount(fromAccountId, normalizedCurrencyId, amount);
        }

        createAccount(fromAccountId);
        createAccount(toAccountId);
        BigDecimal fromBalance = getBalance(fromAccountId, normalizedCurrencyId);
        if (fromBalance.compareTo(normalizedAmount) < 0) {
            return LegacyTransactionResult.failure(normalizedCurrencyId, normalizedAmount, fromBalance, LegacyTransactionFailure.INSUFFICIENT_FUNDS, "Insufficient funds.");
        }

        UUID referenceId = UUID.randomUUID();
        BigDecimal nextFromBalance = fromBalance.subtract(normalizedAmount);
        BigDecimal nextToBalance = getBalance(toAccountId, normalizedCurrencyId).add(normalizedAmount);

        this.balances.get(fromAccountId).put(normalizedCurrencyId, nextFromBalance);
        this.balances.get(toAccountId).put(normalizedCurrencyId, nextToBalance);

        LegacyLedgerEntry outEntry = recordEntry(fromAccountId, toAccountId, normalizedCurrencyId, LegacyTransactionKind.TRANSFER_OUT, normalizedAmount, fromBalance, nextFromBalance, actor, reason, referenceId, null);
        recordEntry(toAccountId, fromAccountId, normalizedCurrencyId, LegacyTransactionKind.TRANSFER_IN, normalizedAmount, nextToBalance.subtract(normalizedAmount), nextToBalance, actor, reason, referenceId, null);
        persistAccount(fromAccountId);
        persistAccount(toAccountId);
        persistHistory(fromAccountId);
        persistHistory(toAccountId);
        return LegacyTransactionResult.success(normalizedCurrencyId, normalizedAmount, nextFromBalance, reason, outEntry);
    }

    synchronized LegacyTransactionResult setBalance(
            final UUID accountId,
            final String currencyId,
            final BigDecimal amount,
            final LegacyTransactionActor actor,
            final String reason
    ) {
        String normalizedCurrencyId = normalizedCurrencyId(currencyId);
        BigDecimal normalizedAmount = validateAmount(normalizedCurrencyId, amount);
        if (normalizedAmount == null) {
            return invalidAmount(accountId, normalizedCurrencyId, amount);
        }

        createAccount(accountId);
        BigDecimal previousBalance = getBalance(accountId, normalizedCurrencyId);
        this.balances.get(accountId).put(normalizedCurrencyId, normalizedAmount);
        BigDecimal zero = BigDecimal.ZERO.setScale(currency(normalizedCurrencyId).getFractionalDigits(), RoundingMode.HALF_UP);
        LegacyLedgerEntry entry = recordEntry(accountId, null, normalizedCurrencyId, LegacyTransactionKind.SET, zero, previousBalance, normalizedAmount, actor, reason, UUID.randomUUID(), null);
        persistAccount(accountId);
        persistHistory(accountId);
        return LegacyTransactionResult.success(normalizedCurrencyId, zero, normalizedAmount, reason, entry);
    }

    synchronized List<LegacyLedgerEntry> recentTransactions(final UUID accountId, final int limit) {
        return filteredTransactions(accountId, 0, limit, null, null);
    }

    synchronized List<LegacyLedgerEntry> filteredTransactions(
            final UUID accountId,
            final int offset,
            final int limit,
            final String currencyId,
            final LegacyTransactionKind kind
    ) {
        List<LegacyLedgerEntry> accountHistory = this.history.get(accountId);
        if (accountHistory == null) {
            return Collections.emptyList();
        }

        List<LegacyLedgerEntry> entries = new ArrayList<LegacyLedgerEntry>(accountHistory);
        Collections.sort(entries, new Comparator<LegacyLedgerEntry>() {
            public int compare(final LegacyLedgerEntry left, final LegacyLedgerEntry right) {
                return Long.compare(right.getCreatedAtEpochMilli(), left.getCreatedAtEpochMilli());
            }
        });
        List<LegacyLedgerEntry> filtered = new ArrayList<LegacyLedgerEntry>();
        String normalizedCurrencyId = currencyId == null || currencyId.trim().isEmpty() ? null : normalizedCurrencyId(currencyId);
        for (LegacyLedgerEntry entry : entries) {
            if (normalizedCurrencyId != null && !normalizedCurrencyId.equals(entry.getCurrencyId())) {
                continue;
            }
            if (kind != null && kind != entry.getKind()) {
                continue;
            }
            filtered.add(entry);
        }
        int safeOffset = Math.max(offset, 0);
        int safeLimit = Math.max(limit, 0);
        if (safeOffset >= filtered.size()) {
            return Collections.emptyList();
        }
        int toIndex = Math.min(filtered.size(), safeOffset + safeLimit);
        if (safeLimit == 0) {
            return Collections.emptyList();
        }
        return new ArrayList<LegacyLedgerEntry>(filtered.subList(safeOffset, toIndex));
    }

    synchronized List<LegacyAccountSnapshot> topAccounts(final String currencyId, final int limit) {
        return topAccounts(currencyId, limit, new java.util.HashSet<UUID>());
    }

    synchronized List<LegacyAccountSnapshot> topAccounts(final String currencyId, final int limit, final java.util.Set<UUID> excludedAccounts) {
        final String normalizedCurrencyId = normalizedCurrencyId(currencyId);
        List<LegacyAccountSnapshot> accounts = new ArrayList<LegacyAccountSnapshot>();
        for (Map.Entry<UUID, Map<String, BigDecimal>> entry : this.balances.entrySet()) {
            if (excludedAccounts.contains(entry.getKey())) {
                continue;
            }
            BigDecimal balance = entry.getValue().get(normalizedCurrencyId);
            if (balance == null) {
                balance = currency(normalizedCurrencyId).getStartingBalance();
            }
            accounts.add(new LegacyAccountSnapshot(entry.getKey(), normalizedCurrencyId, balance));
        }

        Collections.sort(accounts, new Comparator<LegacyAccountSnapshot>() {
            public int compare(final LegacyAccountSnapshot left, final LegacyAccountSnapshot right) {
                return right.getBalance().compareTo(left.getBalance());
            }
        });
        int safeLimit = Math.max(limit, 0);
        if (accounts.size() > safeLimit) {
            return new ArrayList<LegacyAccountSnapshot>(accounts.subList(0, safeLimit));
        }
        return accounts;
    }

    synchronized LegacyLedgerEntry findEntry(final UUID entryId) {
        for (List<LegacyLedgerEntry> entries : this.history.values()) {
            for (LegacyLedgerEntry entry : entries) {
                if (entry.getEntryId().equals(entryId)) {
                    return entry;
                }
            }
        }
        return null;
    }

    synchronized LegacyTransactionResult rollback(final UUID entryId, final LegacyTransactionActor actor, final String reason) {
        LegacyLedgerEntry target = findEntry(entryId);
        if (target == null) {
            return LegacyTransactionResult.failure(this.defaultCurrencyId, BigDecimal.ZERO, BigDecimal.ZERO, LegacyTransactionFailure.NOT_FOUND, "No Coffers Legacy ledger entry was found for that id.");
        }
        if (hasReversalForReference(target.getReferenceId())) {
            return LegacyTransactionResult.failure(target.getCurrencyId(), target.getAmount(), target.getResultingBalance(), LegacyTransactionFailure.ROLLBACK_UNAVAILABLE, "That Coffers Legacy transaction has already been rolled back.");
        }

        String rollbackReason = reason == null || reason.trim().isEmpty()
                ? "Rollback of transaction " + target.getEntryId().toString()
                : reason;

        switch (target.getKind()) {
            case DEPOSIT:
                return rollbackWithdrawal(target, actor, rollbackReason);
            case WITHDRAWAL:
                return rollbackDeposit(target, actor, rollbackReason);
            case TRANSFER_IN:
            case TRANSFER_OUT:
                return rollbackTransfer(target, actor, rollbackReason);
            case SET:
            default:
                return rollbackSet(target, actor, rollbackReason);
        }
    }

    synchronized void purgeAccount(final UUID accountId) {
        this.balances.remove(accountId);
        this.history.remove(accountId);
        persistAccountSnapshot(accountId, Collections.<String, BigDecimal>emptyMap());
        persistHistorySnapshot(accountId, Collections.<LegacyLedgerEntry>emptyList());
    }

    String format(final String currencyId, final BigDecimal amount) {
        LegacyCurrencyDefinition currency = currency(currencyId);
        BigDecimal normalized = normalize(currency.getId(), amount);
        LegacyCurrencyFormat format = currency.getFormat();

        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.US);
        StringBuilder pattern = new StringBuilder(format.isUseGrouping() ? "#,##0" : "0");
        if (currency.getFractionalDigits() > 0) {
            pattern.append('.');
            for (int index = 0; index < currency.getFractionalDigits(); index++) {
                pattern.append(format.isShowTrailingZeros() ? '0' : '#');
            }
        }

        DecimalFormat decimalFormat = new DecimalFormat(pattern.toString(), symbols);
        decimalFormat.setRoundingMode(RoundingMode.HALF_UP);
        String formattedAmount = decimalFormat.format(normalized);
        String unit = normalized.compareTo(BigDecimal.ONE.setScale(currency.getFractionalDigits(), RoundingMode.HALF_UP)) == 0
                ? currency.getSingularName()
                : currency.getPluralName();

        String amountWithSymbol;
        if (currency.getSymbol() == null || currency.getSymbol().trim().isEmpty()) {
            amountWithSymbol = formattedAmount;
        } else if (format.isSymbolFirst()) {
            amountWithSymbol = currency.getSymbol() + (format.isSpaceBetweenSymbolAndAmount() ? " " : "") + formattedAmount;
        } else {
            amountWithSymbol = formattedAmount + (format.isSpaceBetweenSymbolAndAmount() ? " " : "") + currency.getSymbol();
        }

        return amountWithSymbol + (format.isSpaceBetweenAmountAndName() ? " " : "") + unit;
    }

    void close() {
        try {
            this.storage.close();
        } catch (Exception exception) {
            this.logger.warning("Failed to close Coffers Legacy storage cleanly: " + exception.getMessage());
        }
    }

    synchronized LegacyStorageSnapshot snapshot() {
        Map<UUID, Map<String, BigDecimal>> balanceCopy = new LinkedHashMap<UUID, Map<String, BigDecimal>>();
        for (Map.Entry<UUID, Map<String, BigDecimal>> entry : this.balances.entrySet()) {
            balanceCopy.put(entry.getKey(), new LinkedHashMap<String, BigDecimal>(entry.getValue()));
        }

        Map<UUID, List<LegacyLedgerEntry>> historyCopy = new LinkedHashMap<UUID, List<LegacyLedgerEntry>>();
        for (Map.Entry<UUID, List<LegacyLedgerEntry>> entry : this.history.entrySet()) {
            historyCopy.put(entry.getKey(), new ArrayList<LegacyLedgerEntry>(entry.getValue()));
        }
        return new LegacyStorageSnapshot(balanceCopy, historyCopy);
    }

    synchronized void replaceSnapshot(final LegacyStorageSnapshot snapshot) {
        Set<UUID> previousAccountIds = new LinkedHashSet<UUID>();
        previousAccountIds.addAll(this.balances.keySet());
        previousAccountIds.addAll(this.history.keySet());

        this.balances.clear();
        this.history.clear();

        for (Map.Entry<UUID, Map<String, BigDecimal>> entry : snapshot.getBalances().entrySet()) {
            Map<String, BigDecimal> accountBalances = new ConcurrentHashMap<String, BigDecimal>();
            for (LegacyCurrencyDefinition currency : this.currencies.values()) {
                BigDecimal importedBalance = entry.getValue().get(currency.getId());
                accountBalances.put(
                        currency.getId(),
                        normalize(currency.getId(), importedBalance == null ? currency.getStartingBalance() : importedBalance)
                );
            }
            this.balances.put(entry.getKey(), accountBalances);
            persistAccount(entry.getKey());
        }

        for (Map.Entry<UUID, List<LegacyLedgerEntry>> entry : snapshot.getHistory().entrySet()) {
            this.history.put(entry.getKey(), new ArrayList<LegacyLedgerEntry>(entry.getValue()));
            persistHistory(entry.getKey());
        }

        Set<UUID> importedAccountIds = new LinkedHashSet<UUID>();
        importedAccountIds.addAll(snapshot.getBalances().keySet());
        importedAccountIds.addAll(snapshot.getHistory().keySet());
        previousAccountIds.removeAll(importedAccountIds);

        for (UUID removedAccountId : previousAccountIds) {
            persistAccountSnapshot(removedAccountId, Collections.<String, BigDecimal>emptyMap());
            persistHistorySnapshot(removedAccountId, Collections.<LegacyLedgerEntry>emptyList());
        }
    }

    private LegacyLedgerEntry recordEntry(
            final UUID accountId,
            final UUID counterpartyAccountId,
            final String currencyId,
            final LegacyTransactionKind kind,
            final BigDecimal amount,
            final BigDecimal previousBalance,
            final BigDecimal resultingBalance,
            final LegacyTransactionActor actor,
            final String reason,
            final UUID referenceId,
            final UUID reversalOfReferenceId
    ) {
        LegacyTransactionActor normalizedActor = normalizeActor(actor);
        LegacyLedgerEntry entry = new LegacyLedgerEntry(
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

        List<LegacyLedgerEntry> accountHistory = this.history.get(accountId);
        if (accountHistory == null) {
            accountHistory = new ArrayList<LegacyLedgerEntry>();
            this.history.put(accountId, accountHistory);
        }
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

    private LegacyTransactionResult rollbackWithdrawal(final LegacyLedgerEntry target, final LegacyTransactionActor actor, final String reason) {
        BigDecimal amount = normalize(target.getCurrencyId(), target.getAmount());
        BigDecimal currentBalance = getBalance(target.getAccountId(), target.getCurrencyId());
        if (currentBalance.compareTo(amount) < 0) {
            return LegacyTransactionResult.failure(target.getCurrencyId(), amount, currentBalance, LegacyTransactionFailure.INSUFFICIENT_FUNDS, "Not enough balance remains to roll back that deposit.");
        }

        this.balances.get(target.getAccountId()).put(target.getCurrencyId(), currentBalance.subtract(amount));
        LegacyLedgerEntry entry = recordEntry(target.getAccountId(), null, target.getCurrencyId(), LegacyTransactionKind.WITHDRAWAL, amount, currentBalance, currentBalance.subtract(amount), actor, reason, UUID.randomUUID(), target.getReferenceId());
        persistAccount(target.getAccountId());
        persistHistory(target.getAccountId());
        return LegacyTransactionResult.success(target.getCurrencyId(), amount, currentBalance.subtract(amount), reason, entry);
    }

    private LegacyTransactionResult rollbackDeposit(final LegacyLedgerEntry target, final LegacyTransactionActor actor, final String reason) {
        BigDecimal amount = normalize(target.getCurrencyId(), target.getAmount());
        BigDecimal currentBalance = getBalance(target.getAccountId(), target.getCurrencyId());
        this.balances.get(target.getAccountId()).put(target.getCurrencyId(), currentBalance.add(amount));
        LegacyLedgerEntry entry = recordEntry(target.getAccountId(), null, target.getCurrencyId(), LegacyTransactionKind.DEPOSIT, amount, currentBalance, currentBalance.add(amount), actor, reason, UUID.randomUUID(), target.getReferenceId());
        persistAccount(target.getAccountId());
        persistHistory(target.getAccountId());
        return LegacyTransactionResult.success(target.getCurrencyId(), amount, currentBalance.add(amount), reason, entry);
    }

    private LegacyTransactionResult rollbackTransfer(final LegacyLedgerEntry target, final LegacyTransactionActor actor, final String reason) {
        if (target.getCounterpartyAccountId() == null) {
            return LegacyTransactionResult.failure(target.getCurrencyId(), target.getAmount(), target.getResultingBalance(), LegacyTransactionFailure.ROLLBACK_UNAVAILABLE, "That transfer cannot be rolled back because the other account is missing.");
        }

        UUID fromAccountId = target.getAccountId();
        UUID toAccountId = target.getCounterpartyAccountId();
        BigDecimal amount = normalize(target.getCurrencyId(), target.getAmount());
        BigDecimal fromBalance = getBalance(fromAccountId, target.getCurrencyId());
        if (fromBalance.compareTo(amount) < 0) {
            return LegacyTransactionResult.failure(target.getCurrencyId(), amount, fromBalance, LegacyTransactionFailure.INSUFFICIENT_FUNDS, "The receiving account no longer has enough balance to roll back this transfer.");
        }

        BigDecimal toBalance = getBalance(toAccountId, target.getCurrencyId());
        UUID referenceId = UUID.randomUUID();
        BigDecimal nextFromBalance = fromBalance.subtract(amount);
        BigDecimal nextToBalance = toBalance.add(amount);
        this.balances.get(fromAccountId).put(target.getCurrencyId(), nextFromBalance);
        this.balances.get(toAccountId).put(target.getCurrencyId(), nextToBalance);
        LegacyLedgerEntry outEntry = recordEntry(fromAccountId, toAccountId, target.getCurrencyId(), LegacyTransactionKind.TRANSFER_OUT, amount, fromBalance, nextFromBalance, actor, reason, referenceId, target.getReferenceId());
        recordEntry(toAccountId, fromAccountId, target.getCurrencyId(), LegacyTransactionKind.TRANSFER_IN, amount, toBalance, nextToBalance, actor, reason, referenceId, target.getReferenceId());
        persistAccount(fromAccountId);
        persistAccount(toAccountId);
        persistHistory(fromAccountId);
        persistHistory(toAccountId);
        return LegacyTransactionResult.success(target.getCurrencyId(), amount, nextFromBalance, reason, outEntry);
    }

    private LegacyTransactionResult rollbackSet(final LegacyLedgerEntry target, final LegacyTransactionActor actor, final String reason) {
        BigDecimal previousBalance = normalize(target.getCurrencyId(), target.getPreviousBalance());
        this.balances.get(target.getAccountId()).put(target.getCurrencyId(), previousBalance);
        BigDecimal zero = BigDecimal.ZERO.setScale(currency(target.getCurrencyId()).getFractionalDigits(), RoundingMode.HALF_UP);
        LegacyLedgerEntry entry = recordEntry(target.getAccountId(), null, target.getCurrencyId(), LegacyTransactionKind.SET, zero, target.getResultingBalance(), previousBalance, actor, reason, UUID.randomUUID(), target.getReferenceId());
        persistAccount(target.getAccountId());
        persistHistory(target.getAccountId());
        return LegacyTransactionResult.success(target.getCurrencyId(), zero, previousBalance, reason, entry);
    }

    private boolean hasReversalForReference(final UUID referenceId) {
        for (List<LegacyLedgerEntry> entries : this.history.values()) {
            for (LegacyLedgerEntry entry : entries) {
                if (referenceId.equals(entry.getReversalOfReferenceId())) {
                    return true;
                }
            }
        }
        return false;
    }

    private LegacyTransactionResult invalidAmount(final UUID accountId, final String currencyId, final BigDecimal attemptedAmount) {
        BigDecimal fallback = attemptedAmount == null ? BigDecimal.ZERO : attemptedAmount;
        return LegacyTransactionResult.failure(currencyId, normalize(currencyId, fallback), getBalance(accountId, currencyId), LegacyTransactionFailure.INVALID_AMOUNT, "Amount must be zero or greater.");
    }

    private BigDecimal normalize(final String currencyId, final BigDecimal amount) {
        LegacyCurrencyDefinition currency = currency(currencyId);
        return amount.setScale(currency.getFractionalDigits(), RoundingMode.HALF_UP);
    }

    private String normalizedCurrencyId(final String currencyId) {
        String normalized = currencyId == null ? this.defaultCurrencyId : currencyId.toLowerCase(Locale.ROOT);
        if (!this.currencies.containsKey(normalized)) {
            throw new IllegalArgumentException("Unknown currency: " + currencyId);
        }
        return normalized;
    }

    private void persistAccount(final UUID accountId) {
        persistAccountSnapshot(accountId, this.balances.get(accountId));
    }

    private void persistAccountSnapshot(final UUID accountId, final Map<String, BigDecimal> balancesSnapshot) {
        try {
            this.storage.saveAccount(accountId, new LinkedHashMap<String, BigDecimal>(balancesSnapshot));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to persist Coffers Legacy account " + accountId, exception);
        }
    }

    private void persistHistory(final UUID accountId) {
        List<LegacyLedgerEntry> accountHistory = this.history.get(accountId);
        if (accountHistory == null) {
            accountHistory = Collections.emptyList();
        }
        persistHistorySnapshot(accountId, accountHistory);
    }

    private void persistHistorySnapshot(final UUID accountId, final List<LegacyLedgerEntry> historySnapshot) {
        try {
            this.storage.saveHistory(accountId, new ArrayList<LegacyLedgerEntry>(historySnapshot));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to persist Coffers Legacy history for " + accountId, exception);
        }
    }

    private LegacyTransactionActor normalizeActor(final LegacyTransactionActor actor) {
        return actor != null ? actor : LegacyTransactionActor.system("coffers-legacy-runtime");
    }
}
