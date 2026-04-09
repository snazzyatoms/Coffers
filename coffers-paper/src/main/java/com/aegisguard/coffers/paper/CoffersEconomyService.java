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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

final class CoffersEconomyService implements CoffersEconomy {

    private final Map<UUID, Map<String, BigDecimal>> balances = new ConcurrentHashMap<>();
    private final Map<UUID, List<LedgerEntry>> history = new ConcurrentHashMap<>();
    private final Map<String, CurrencyDefinition> currencies;
    private final String defaultCurrencyId;
    private final EconomyStorage storage;
    private final int historyLimit;
    private final Logger logger;

    CoffersEconomyService(
            final Collection<CurrencyDefinition> currencies,
            final String defaultCurrencyId,
            final EconomyStorage storage,
            final int historyLimit,
            final StorageSnapshot snapshot,
            final Logger logger
    ) {
        this.currencies = new LinkedHashMap<>();
        for (final CurrencyDefinition currency : currencies) {
            this.currencies.put(currency.id().toLowerCase(Locale.ROOT), currency);
        }
        this.defaultCurrencyId = defaultCurrencyId.toLowerCase(Locale.ROOT);
        this.storage = storage;
        this.historyLimit = historyLimit;
        this.logger = logger;
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
        final BigDecimal nextBalance = getBalance(accountId, normalizedCurrencyId).add(normalizedAmount);
        this.balances.get(accountId).put(normalizedCurrencyId, nextBalance);

        final LedgerEntry entry = recordEntry(
                accountId,
                null,
                normalizedCurrencyId,
                TransactionKind.DEPOSIT,
                normalizedAmount,
                nextBalance,
                actor,
                reason,
                UUID.randomUUID()
        );
        persistAccount(accountId);
        persistHistory(accountId);
        return TransactionResult.success(normalizedCurrencyId, normalizedAmount, nextBalance, reason, entry);
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
                nextBalance,
                actor,
                reason,
                UUID.randomUUID()
        );
        persistAccount(accountId);
        persistHistory(accountId);
        return TransactionResult.success(normalizedCurrencyId, normalizedAmount, nextBalance, reason, entry);
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
                nextFromBalance,
                actor,
                reason,
                referenceId
        );
        recordEntry(
                toAccountId,
                fromAccountId,
                normalizedCurrencyId,
                TransactionKind.TRANSFER_IN,
                normalizedAmount,
                nextToBalance,
                actor,
                reason,
                referenceId
        );

        persistAccount(fromAccountId);
        persistAccount(toAccountId);
        persistHistory(fromAccountId);
        persistHistory(toAccountId);
        return TransactionResult.success(normalizedCurrencyId, normalizedAmount, nextFromBalance, reason, outEntry);
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
        this.balances.get(accountId).put(normalizedCurrencyId, normalizedAmount);
        final LedgerEntry entry = recordEntry(
                accountId,
                null,
                normalizedCurrencyId,
                TransactionKind.SET,
                BigDecimal.ZERO.setScale(currency(normalizedCurrencyId).orElseThrow().fractionalDigits(), RoundingMode.HALF_UP),
                normalizedAmount,
                actor,
                reason,
                UUID.randomUUID()
        );
        persistAccount(accountId);
        persistHistory(accountId);
        return TransactionResult.success(normalizedCurrencyId, BigDecimal.ZERO, normalizedAmount, reason, entry);
    }

    @Override
    public synchronized List<LedgerEntry> recentTransactions(final UUID accountId, final int limit) {
        final List<LedgerEntry> entries = new ArrayList<>(this.history.getOrDefault(accountId, List.of()));
        entries.sort(Comparator.comparingLong(LedgerEntry::createdAtEpochMilli).reversed());
        return entries.stream().limit(Math.max(limit, 0)).toList();
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

    private LedgerEntry recordEntry(
            final UUID accountId,
            final UUID counterpartyAccountId,
            final String currencyId,
            final TransactionKind kind,
            final BigDecimal amount,
            final BigDecimal resultingBalance,
            final TransactionActor actor,
            final String reason,
            final UUID referenceId
    ) {
        final LedgerEntry entry = new LedgerEntry(
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

    private String normalizedCurrencyId(final String currencyId) {
        final String normalized = currencyId == null ? this.defaultCurrencyId : currencyId.toLowerCase(Locale.ROOT);
        if (!this.currencies.containsKey(normalized)) {
            throw new IllegalArgumentException("Unknown currency: " + currencyId);
        }
        return normalized;
    }

    private void persistAccount(final UUID accountId) {
        try {
            this.storage.saveAccount(accountId, new LinkedHashMap<>(this.balances.get(accountId)));
        } catch (final Exception exception) {
            throw new IllegalStateException("Failed to persist Coffers account " + accountId, exception);
        }
    }

    private void persistHistory(final UUID accountId) {
        try {
            this.storage.saveHistory(accountId, List.copyOf(this.history.getOrDefault(accountId, List.of())));
        } catch (final Exception exception) {
            throw new IllegalStateException("Failed to persist Coffers history for " + accountId, exception);
        }
    }
}
