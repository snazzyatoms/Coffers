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
                nextBalance,
                actor,
                reason,
                UUID.randomUUID()
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
                nextFromBalance,
                actor,
                reason,
                referenceId
        );
        final LedgerEntry inEntry = recordEntry(
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
        final List<LedgerEntry> entries = new ArrayList<>(this.history.getOrDefault(accountId, List.of()));
        entries.sort(Comparator.comparingLong(LedgerEntry::createdAtEpochMilli).reversed());
        return entries.stream()
                .skip(Math.max(offset, 0))
                .limit(Math.max(limit, 0))
                .toList();
    }

    @Override
    public synchronized List<AccountSnapshot> topAccounts(final String currencyId, final int limit) {
        final String normalizedCurrencyId = normalizedCurrencyId(currencyId);
        return this.balances.entrySet().stream()
                .map(entry -> new AccountSnapshot(
                        entry.getKey(),
                        normalizedCurrencyId,
                        entry.getValue().getOrDefault(normalizedCurrencyId, startingBalance(normalizedCurrencyId))
                ))
                .sorted(Comparator.comparing(AccountSnapshot::balance).reversed())
                .limit(Math.max(limit, 0))
                .toList();
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
                actor
        ));
    }
}
