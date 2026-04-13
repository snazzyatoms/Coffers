package com.aegisguard.coffers.paper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

final class CoffersMessagePalette {

    private final Map<String, String> styleTokens;

    private CoffersMessagePalette(final Map<String, String> styleTokens) {
        this.styleTokens = styleTokens;
    }

    static CoffersMessagePalette fromConfig(final FileConfiguration config) {
        final Map<String, String> tokens = new LinkedHashMap<>();
        tokens.put("prefix", config.getString("chat-style.prefix", "&6[Coffers]&r "));
        tokens.put("primary", config.getString("chat-style.primary", "&f"));
        tokens.put("secondary", config.getString("chat-style.secondary", "&7"));
        tokens.put("accent", config.getString("chat-style.accent", "&6"));
        tokens.put("success", config.getString("chat-style.success", "&a"));
        tokens.put("error", config.getString("chat-style.error", "&c"));
        tokens.put("warning", config.getString("chat-style.warning", "&e"));
        tokens.put("highlight", config.getString("chat-style.highlight", "&b"));
        tokens.put("usage", config.getString("chat-style.usage", "&e"));
        tokens.put("bullet", config.getString("chat-style.bullet", "&8- &r"));
        tokens.put("reset", "&r");
        return new CoffersMessagePalette(tokens);
    }

    void send(final CommandSender sender, final String template) {
        sender.sendMessage(render("<prefix>" + template, Map.of()));
    }

    void send(final CommandSender sender, final String template, final Map<String, String> placeholders) {
        sender.sendMessage(render("<prefix>" + template, placeholders));
    }

    String render(final String template) {
        return render(template, Map.of());
    }

    String render(final String template, final Map<String, String> placeholders) {
        String resolved = template;
        for (final Map.Entry<String, String> entry : placeholders.entrySet()) {
            resolved = resolved.replace("%" + entry.getKey() + "%", Objects.toString(entry.getValue(), ""));
        }
        for (final Map.Entry<String, String> entry : this.styleTokens.entrySet()) {
            resolved = resolved.replace("<" + entry.getKey() + ">", entry.getValue());
        }
        return ChatColor.translateAlternateColorCodes('&', resolved);
    }
}
