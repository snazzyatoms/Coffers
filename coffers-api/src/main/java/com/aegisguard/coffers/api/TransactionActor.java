package com.aegisguard.coffers.api;

import java.util.UUID;

public record TransactionActor(
        TransactionActorType type,
        UUID actorId,
        String actorName,
        String source
) {

    public static TransactionActor system(final String source) {
        return new TransactionActor(TransactionActorType.SYSTEM, null, "System", source);
    }

    public static TransactionActor console(final String source) {
        return new TransactionActor(TransactionActorType.CONSOLE, null, "Console", source);
    }

    public static TransactionActor player(final UUID actorId, final String actorName, final String source) {
        return new TransactionActor(TransactionActorType.PLAYER, actorId, actorName, source);
    }

    public static TransactionActor plugin(final String actorName, final String source) {
        return new TransactionActor(TransactionActorType.PLUGIN, null, actorName, source);
    }

    public static TransactionActor migration(final String source) {
        return new TransactionActor(TransactionActorType.MIGRATION, null, "Migration", source);
    }

    public static TransactionActor vault(final String source) {
        return new TransactionActor(TransactionActorType.VAULT, null, "Vault", source);
    }
}
