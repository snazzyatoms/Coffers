package com.aegisguard.coffers.paper;

import com.aegisguard.coffers.api.CoffersEconomy;
import com.aegisguard.coffers.api.TransactionFailure;
import com.aegisguard.coffers.api.TransactionResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class InMemoryCoffersEconomy implements CoffersEconomy {

    private final Map<UUID, BigDecimal> balances = new ConcurrentHashMap<>();
    private final String singular;
    private final String plural;
    private final String symbol;
    private final BigDecimal startingBalance;
    private final int fractionalDigits;

    InMemoryCoffersEconomy(
            final String singular,
            final String plural,
            final String symbol,
            final BigDecimal startingBalance,
            final int fractionalDigits
    ) {
        this.singular = singular;
        this.plural = plural;
        this.symbol = symbol;
        this.startingBalance = normalize(startingBalance, fractionalDigits);
        this.fractionalDigits = fractionalDigits;
    }

    @Override
    public String currencyNameSingular() {
        return this.singular;
    }

    @Override
    public String currencyNamePlural() {
        return this.plural;
    }

    @Override
    public String currencySymbol() {
        return this.symbol;
    }

    @Override
    public int fractionalDigits() {
        return this.fractionalDigits;
    }

    @Override
    public boolean hasAccount(final UUID accountId) {
        return this.balances.containsKey(accountId);
    }

    @Override
    public void createAccount(final UUID accountId) {
        this.balances.putIfAbsent(accountId, this.startingBalance);
    }

    @Override
    public synchronized BigDecimal getBalance(final UUID accountId) {
        createAccount(accountId);
        return this.balances.get(accountId);
    }

    @Override
    public synchronized TransactionResult deposit(final UUID accountId, final BigDecimal amount, final String reason) {
        final BigDecimal normalizedAmount = validateAmount(accountId, amount);
        if (normalizedAmount == null) {
            return invalidAmount(accountId, amount);
        }

        final BigDecimal nextBalance = getBalance(accountId).add(normalizedAmount);
        this.balances.put(accountId, nextBalance);
        return TransactionResult.success(normalizedAmount, nextBalance, reason);
    }

    @Override
    public synchronized TransactionResult withdraw(final UUID accountId, final BigDecimal amount, final String reason) {
        final BigDecimal normalizedAmount = validateAmount(accountId, amount);
        if (normalizedAmount == null) {
            return invalidAmount(accountId, amount);
        }

        final BigDecimal currentBalance = getBalance(accountId);
        if (currentBalance.compareTo(normalizedAmount) < 0) {
            return TransactionResult.failure(
                    normalizedAmount,
                    currentBalance,
                    TransactionFailure.INSUFFICIENT_FUNDS,
                    "Insufficient funds."
            );
        }

        final BigDecimal nextBalance = currentBalance.subtract(normalizedAmount);
        this.balances.put(accountId, nextBalance);
        return TransactionResult.success(normalizedAmount, nextBalance, reason);
    }

    @Override
    public synchronized TransactionResult transfer(
            final UUID fromAccountId,
            final UUID toAccountId,
            final BigDecimal amount,
            final String reason
    ) {
        final TransactionResult withdrawal = withdraw(fromAccountId, amount, reason);
        if (!withdrawal.successful()) {
            return withdrawal;
        }

        final TransactionResult deposit = deposit(toAccountId, amount, reason);
        if (!deposit.successful()) {
            this.balances.put(fromAccountId, withdrawal.balance().add(withdrawal.amount()));
            return deposit;
        }

        return TransactionResult.success(deposit.amount(), getBalance(fromAccountId), reason);
    }

    @Override
    public synchronized TransactionResult setBalance(final UUID accountId, final BigDecimal amount, final String reason) {
        final BigDecimal normalizedAmount = validateAmount(accountId, amount);
        if (normalizedAmount == null) {
            return invalidAmount(accountId, amount);
        }

        this.balances.put(accountId, normalizedAmount);
        return TransactionResult.success(BigDecimal.ZERO.setScale(this.fractionalDigits, RoundingMode.HALF_UP), normalizedAmount, reason);
    }

    @Override
    public String format(final BigDecimal amount) {
        final BigDecimal normalized = normalize(amount, this.fractionalDigits);
        final String unit = normalized.compareTo(BigDecimal.ONE.setScale(this.fractionalDigits, RoundingMode.HALF_UP)) == 0
                ? this.singular
                : this.plural;
        return this.symbol + normalized.toPlainString() + " " + unit;
    }

    private BigDecimal validateAmount(final UUID accountId, final BigDecimal amount) {
        createAccount(accountId);
        if (amount == null || amount.signum() < 0) {
            return null;
        }
        return normalize(amount, this.fractionalDigits);
    }

    private TransactionResult invalidAmount(final UUID accountId, final BigDecimal attemptedAmount) {
        return TransactionResult.failure(
                attemptedAmount == null ? BigDecimal.ZERO.setScale(this.fractionalDigits, RoundingMode.HALF_UP) : attemptedAmount,
                getBalance(accountId),
                TransactionFailure.INVALID_AMOUNT,
                "Amount must be zero or greater."
        );
    }

    private BigDecimal normalize(final BigDecimal amount, final int scale) {
        return amount.setScale(scale, RoundingMode.HALF_UP);
    }
}
