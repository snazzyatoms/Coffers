package com.aegisguard.coffers.legacy;

import java.util.UUID;

final class LegacyTransactionActor {

    private final LegacyTransactionActorType type;
    private final UUID actorId;
    private final String actorName;
    private final String source;

    LegacyTransactionActor(final LegacyTransactionActorType type, final UUID actorId, final String actorName, final String source) {
        this.type = type;
        this.actorId = actorId;
        this.actorName = actorName;
        this.source = source;
    }

    static LegacyTransactionActor system(final String source) {
        return new LegacyTransactionActor(LegacyTransactionActorType.SYSTEM, null, "System", source);
    }

    static LegacyTransactionActor console(final String source) {
        return new LegacyTransactionActor(LegacyTransactionActorType.CONSOLE, null, "Console", source);
    }

    static LegacyTransactionActor player(final UUID actorId, final String actorName, final String source) {
        return new LegacyTransactionActor(LegacyTransactionActorType.PLAYER, actorId, actorName, source);
    }

    static LegacyTransactionActor migration(final String source) {
        return new LegacyTransactionActor(LegacyTransactionActorType.MIGRATION, null, "Migration", source);
    }

    static LegacyTransactionActor vault(final String source) {
        return new LegacyTransactionActor(LegacyTransactionActorType.VAULT, null, "Vault", source);
    }

    LegacyTransactionActorType getType() {
        return this.type;
    }

    UUID getActorId() {
        return this.actorId;
    }

    String getActorName() {
        return this.actorName;
    }

    String getSource() {
        return this.source;
    }
}
