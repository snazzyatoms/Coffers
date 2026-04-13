package com.aegisguard.coffers.paper;

import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

final class CoffersPlayerAccountListener implements Listener {

    private final CoffersPlugin plugin;

    CoffersPlayerAccountListener(final CoffersPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        bootstrapPlayer(event.getPlayer());
    }

    void bootstrapPlayer(final Player player) {
        final UUID playerId = player.getUniqueId();
        this.plugin.economyService().createAccount(playerId);
        if (!this.plugin.autoCreatePlayerBanks()) {
            return;
        }

        final UUID bankAccountId = this.plugin.bankRegistry().ensurePersonalBank(
                playerId,
                player.getName(),
                this.plugin.playerBankSuffix()
        );
        this.plugin.economyService().createAccount(bankAccountId);
    }
}
