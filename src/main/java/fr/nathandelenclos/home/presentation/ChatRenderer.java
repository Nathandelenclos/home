package fr.nathandelenclos.home.presentation;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

final class ChatRenderer {

    private static final DateTimeFormatter HOUR_MINUTE = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter HOUR_MINUTE_SECOND = DateTimeFormatter.ofPattern("HH:mm:ss");

    String formatMarkdownLike(String input) {
        String output = input;
        output = output.replaceAll("\\*\\*(.+?)\\*\\*", org.bukkit.ChatColor.BOLD + "$1" + org.bukkit.ChatColor.RESET);
        output = output.replaceAll("\\*(.+?)\\*", org.bukkit.ChatColor.ITALIC + "$1" + org.bukkit.ChatColor.RESET);
        output = output.replaceAll("~~(.+?)~~", org.bukkit.ChatColor.STRIKETHROUGH + "$1" + org.bukkit.ChatColor.RESET);
        output = output.replaceAll("`(.+?)`", org.bukkit.ChatColor.GRAY + "$1" + org.bukkit.ChatColor.RESET);
        return output;
    }

    Set<Player> detectMentionedPlayers(String rawMessage) {
        Set<Player> mentioned = new HashSet<>();
        for (String token : rawMessage.split("\\s+")) {
            if (!token.startsWith("@") || token.length() <= 1) {
                continue;
            }
            String candidate = token.substring(1).replaceAll("[^A-Za-z0-9_]", "");
            Player player = Bukkit.getPlayerExact(candidate);
            if (player != null) {
                mentioned.add(player);
            }
        }
        return mentioned;
    }

    BaseComponent[] buildPublicMessage(Player sender, String formattedBody) {
        ComponentBuilder builder = new ComponentBuilder();

        String nowShort = LocalTime.now().format(HOUR_MINUTE);
        String nowLong = LocalTime.now().format(HOUR_MINUTE_SECOND);

        builder.append("[" + nowShort + "] ").color(ChatColor.DARK_GRAY);

        TextComponent name = new TextComponent(sender.getName());
        name.setColor(ChatColor.AQUA);
        name.setBold(true);
        name.setHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("Heure exacte: " + nowLong + "\\nClic gauche: pre-remplir /msg\\nProfil: /playercard " + sender.getName())
                        .color(ChatColor.GRAY)
                        .create()
        ));
        name.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/msg " + sender.getName() + " "));
        builder.append(name).append(" ");

        builder.append(": ").color(ChatColor.WHITE);
        for (BaseComponent part : TextComponent.fromLegacyText(highlightMentions(formattedBody))) {
            builder.append(part);
        }

        return builder.create();
    }

    BaseComponent[] buildPrivateMessage(boolean outgoing, Player from, Player to, String formattedBody) {
        String prefix = outgoing ? "[MP -> " + to.getName() + "] " : "[MP <- " + from.getName() + "] ";
        ComponentBuilder builder = new ComponentBuilder(prefix).color(ChatColor.LIGHT_PURPLE);
        for (BaseComponent part : TextComponent.fromLegacyText(highlightMentions(formattedBody))) {
            builder.append(part);
        }
        return builder.create();
    }

    private String highlightMentions(String formattedMessage) {
        String result = formattedMessage;
        for (Player online : Bukkit.getOnlinePlayers()) {
            String plain = "@" + online.getName();
            String highlighted = org.bukkit.ChatColor.YELLOW + "@" + online.getName() + org.bukkit.ChatColor.RESET;
            result = result.replace(plain, highlighted);
        }
        return result;
    }
}
