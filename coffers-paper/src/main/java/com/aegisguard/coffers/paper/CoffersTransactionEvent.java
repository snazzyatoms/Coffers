package com.aegisguard.coffers.paper;

import com.aegisguard.coffers.api.TransactionActor;
import com.aegisguard.coffers.api.TransactionKind;
import com.aegisguard.coffers.api.TransactionResult;
import java.util.UUID;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class CoffersTransactionEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID accountId;
    private final UUID counterpartyAccountId;
    private final TransactionKind kind;
    private final String currencyId;
    private final TransactionResult result;
    private final TransactionActor actor;

    CoffersTransactionEvent(
            final UUID accountId,
            final UUID counterpartyAccountId,
            final TransactionKind kind,
            final String currencyId,
            final TransactionResult result,
            final TransactionActor actor
    ) {
        this.accountId = accountId;
        this.counterpartyAccountId = counterpartyAccountId;
        this.kind = kind;
        this.currencyId = currencyId;
        this.result = result;
        this.actor = actor;
    }

    public UUID accountId() {
        return this.accountId;
    }

    public UUID counterpartyAccountId() {
        return this.counterpartyAccountId;
    }

    public TransactionKind kind() {
        return this.kind;
    }

    public String currencyId() {
        return this.currencyId;
    }

    public TransactionResult result() {
        return this.result;
    }

    public TransactionActor actor() {
        return this.actor;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
