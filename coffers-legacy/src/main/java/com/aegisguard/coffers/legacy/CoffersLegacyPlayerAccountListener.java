package com.aegisguard.coffers.legacy;

import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

final class CoffersLegacyPlayerAccountListener implements Listener {

    private final CoffersLegacyPlugin plugin;

    CoffersLegacyPlayerAccountListener(final CoffersLegacyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        bootstrapPlayer(event.getPlayer());
    }

    void bootstrapPlayer(final Player player) {
        final UUID playerId = player.getUniqueId();
        this.plugin.economy().createAccount(playerId);
        if (!this.plugin.autoCreatePlayerBanks()) {
            return;
        }

        final UUID bankAccountId = this.plugin.bankRegistry().ensurePersonalBank(
                playerId,
                player.getName(),
                this.plugin.playerBankSuffix()
        );
        this.plugin.economy().createAccount(bankAccountId);
    }
}
