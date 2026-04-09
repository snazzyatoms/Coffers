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
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
        BigDecimal nextBalance = getBalance(accountId, normalizedCurrencyId).add(normalizedAmount);
        this.balances.get(accountId).put(normalizedCurrencyId, nextBalance);
        LegacyLedgerEntry entry = recordEntry(accountId, null, normalizedCurrencyId, LegacyTransactionKind.DEPOSIT, normalizedAmount, nextBalance, actor, reason, UUID.randomUUID());
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
        LegacyLedgerEntry entry = recordEntry(accountId, null, normalizedCurrencyId, LegacyTransactionKind.WITHDRAWAL, normalizedAmount, nextBalance, actor, reason, UUID.randomUUID());
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

        LegacyLedgerEntry outEntry = recordEntry(fromAccountId, toAccountId, normalizedCurrencyId, LegacyTransactionKind.TRANSFER_OUT, normalizedAmount, nextFromBalance, actor, reason, referenceId);
        recordEntry(toAccountId, fromAccountId, normalizedCurrencyId, LegacyTransactionKind.TRANSFER_IN, normalizedAmount, nextToBalance, actor, reason, referenceId);
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
        this.balances.get(accountId).put(normalizedCurrencyId, normalizedAmount);
        BigDecimal zero = BigDecimal.ZERO.setScale(currency(normalizedCurrencyId).getFractionalDigits(), RoundingMode.HALF_UP);
        LegacyLedgerEntry entry = recordEntry(accountId, null, normalizedCurrencyId, LegacyTransactionKind.SET, zero, normalizedAmount, actor, reason, UUID.randomUUID());
        persistAccount(accountId);
        persistHistory(accountId);
        return LegacyTransactionResult.success(normalizedCurrencyId, zero, normalizedAmount, reason, entry);
    }

    synchronized List<LegacyLedgerEntry> recentTransactions(final UUID accountId, final int limit) {
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
        int safeLimit = Math.max(limit, 0);
        if (entries.size() > safeLimit) {
            return new ArrayList<LegacyLedgerEntry>(entries.subList(0, safeLimit));
        }
        return entries;
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

    private LegacyLedgerEntry recordEntry(
            final UUID accountId,
            final UUID counterpartyAccountId,
            final String currencyId,
            final LegacyTransactionKind kind,
            final BigDecimal amount,
            final BigDecimal resultingBalance,
            final LegacyTransactionActor actor,
            final String reason,
            final UUID referenceId
    ) {
        LegacyLedgerEntry entry = new LegacyLedgerEntry(
                UUID.randomUUID(),
                referenceId,
                accountId,
                counterpartyAccountId,
                currencyId,
                kind,
                amount,
                resultingBalance,
                actor,
                reason,
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
        try {
            this.storage.saveAccount(accountId, new LinkedHashMap<String, BigDecimal>(this.balances.get(accountId)));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to persist Coffers Legacy account " + accountId, exception);
        }
    }

    private void persistHistory(final UUID accountId) {
        try {
            List<LegacyLedgerEntry> accountHistory = this.history.get(accountId);
            if (accountHistory == null) {
                accountHistory = Collections.emptyList();
            }
            this.storage.saveHistory(accountId, new ArrayList<LegacyLedgerEntry>(accountHistory));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to persist Coffers Legacy history for " + accountId, exception);
        }
    }
}
