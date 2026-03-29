package fr.nathandelenclos.home.presentation;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ChatFeatureHandler implements Listener, TabExecutor {

    private static final DateTimeFormatter HOUR_MINUTE = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter HOUR_MINUTE_SECOND = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final Set<String> ALLOWED_REACTIONS = Set.of("gg", "lol", "+1", "rip", "fire");

    private final Plugin plugin;
    private final Map<UUID, UUID> lastPrivateContactByPlayer = new HashMap<>();

    private volatile UUID lastPublicMessageSender;

    public ChatFeatureHandler(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer();
        String rawMessage = event.getMessage();
        String formatted = applyMarkdownLikeFormatting(rawMessage);

        event.setCancelled(true);

        Bukkit.getScheduler().runTask(plugin, () -> {
            List<Player> recipients = new ArrayList<>(event.getRecipients());
            if (!recipients.contains(sender)) {
                recipients.add(sender);
            }

            Set<Player> mentionedPlayers = detectMentionedPlayers(rawMessage);
            BaseComponent[] message = buildPublicMessage(sender, formatted);

            for (Player recipient : recipients) {
                recipient.spigot().sendMessage(message);
            }

            for (Player mentioned : mentionedPlayers) {
                mentioned.playSound(mentioned.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.4f);
            }

            lastPublicMessageSender = sender.getUniqueId();
        });
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        return switch (name) {
            case "msg" -> handleMsg(sender, args);
            case "r" -> handleReply(sender, args);
            case "react" -> handleReact(sender, args);
            default -> false;
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);

        if ("msg".equals(name) && args.length == 1) {
            return filterByPrefix(onlinePlayerNamesExcluding(sender), args[0]);
        }

        if ("react".equals(name) && args.length == 1) {
            return filterByPrefix(new ArrayList<>(ALLOWED_REACTIONS), args[0]);
        }

        if ("react".equals(name) && args.length == 2) {
            return filterByPrefix(onlinePlayerNamesExcluding(sender), args[1]);
        }

        return Collections.emptyList();
    }

    private boolean handleMsg(CommandSender sender, String[] args) {
        if (!(sender instanceof Player from)) {
            sender.sendMessage(org.bukkit.ChatColor.RED + "Commande reservee aux joueurs.");
            return true;
        }
        if (args.length < 2) {
            return false;
        }

        Player to = Bukkit.getPlayerExact(args[0]);
        if (to == null) {
            from.sendMessage(org.bukkit.ChatColor.RED + "Joueur introuvable: " + args[0]);
            return true;
        }
        if (from.getUniqueId().equals(to.getUniqueId())) {
            from.sendMessage(org.bukkit.ChatColor.RED + "Tu ne peux pas t'envoyer un message a toi-meme.");
            return true;
        }

        String message = String.join(" ", slice(args, 1));
        String formatted = applyMarkdownLikeFormatting(message);

        from.spigot().sendMessage(buildPrivateMessage(true, from, to, formatted));
        to.spigot().sendMessage(buildPrivateMessage(false, from, to, formatted));

        lastPrivateContactByPlayer.put(from.getUniqueId(), to.getUniqueId());
        lastPrivateContactByPlayer.put(to.getUniqueId(), from.getUniqueId());
        return true;
    }

    private boolean handleReply(CommandSender sender, String[] args) {
        if (!(sender instanceof Player from)) {
            sender.sendMessage(org.bukkit.ChatColor.RED + "Commande reservee aux joueurs.");
            return true;
        }
        if (args.length < 1) {
            return false;
        }

        UUID targetId = lastPrivateContactByPlayer.get(from.getUniqueId());
        if (targetId == null) {
            from.sendMessage(org.bukkit.ChatColor.YELLOW + "Aucun dernier contact pour repondre.");
            return true;
        }

        Player to = Bukkit.getPlayer(targetId);
        if (to == null) {
            from.sendMessage(org.bukkit.ChatColor.RED + "Ton dernier contact n'est plus en ligne.");
            return true;
        }

        String message = String.join(" ", args);
        String formatted = applyMarkdownLikeFormatting(message);

        from.spigot().sendMessage(buildPrivateMessage(true, from, to, formatted));
        to.spigot().sendMessage(buildPrivateMessage(false, from, to, formatted));

        lastPrivateContactByPlayer.put(from.getUniqueId(), to.getUniqueId());
        lastPrivateContactByPlayer.put(to.getUniqueId(), from.getUniqueId());
        return true;
    }

    private boolean handleReact(CommandSender sender, String[] args) {
        if (!(sender instanceof Player from)) {
            sender.sendMessage(org.bukkit.ChatColor.RED + "Commande reservee aux joueurs.");
            return true;
        }
        if (args.length < 1 || args.length > 2) {
            return false;
        }

        String reaction = args[0].toLowerCase(Locale.ROOT);
        if (!ALLOWED_REACTIONS.contains(reaction)) {
            from.sendMessage(org.bukkit.ChatColor.RED + "Reaction invalide. Disponibles: " + String.join(", ", ALLOWED_REACTIONS));
            return true;
        }

        Player target;
        if (args.length == 2) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                from.sendMessage(org.bukkit.ChatColor.RED + "Joueur introuvable: " + args[1]);
                return true;
            }
        } else {
            if (lastPublicMessageSender == null) {
                from.sendMessage(org.bukkit.ChatColor.YELLOW + "Aucun message recent pour reagir.");
                return true;
            }
            target = Bukkit.getPlayer(lastPublicMessageSender);
            if (target == null) {
                from.sendMessage(org.bukkit.ChatColor.YELLOW + "Le joueur cible n'est plus en ligne.");
                return true;
            }
        }

        String displayReaction = "fire".equals(reaction) ? "FIRE" : reaction.toUpperCase(Locale.ROOT);
        TextComponent msg = new TextComponent(org.bukkit.ChatColor.GOLD + "[Reaction] " + org.bukkit.ChatColor.YELLOW + from.getName()
                + org.bukkit.ChatColor.WHITE + " -> " + org.bukkit.ChatColor.AQUA + target.getName()
                + org.bukkit.ChatColor.WHITE + " : " + org.bukkit.ChatColor.GOLD + displayReaction);

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.spigot().sendMessage(msg);
        }
        return true;
    }

    private BaseComponent[] buildPublicMessage(Player sender, String formattedBody) {
        ComponentBuilder builder = new ComponentBuilder();

        String nowShort = LocalTime.now().format(HOUR_MINUTE);
        String nowLong = LocalTime.now().format(HOUR_MINUTE_SECOND);

        builder.append("[" + nowShort + "] ").color(ChatColor.DARK_GRAY);

        TextComponent name = new TextComponent(sender.getName());
        name.setColor(ChatColor.AQUA);
        name.setBold(true);
        name.setHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("Heure exacte: " + nowLong + "\nClick: pre-remplir MP")
                        .color(ChatColor.GRAY)
                        .create()
        ));
        name.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/msg " + sender.getName() + " "));
        builder.append(name).append(" ");

        builder.append(actionButton("[MP]", ChatColor.GREEN, "/msg " + sender.getName() + " ", "Message prive"));
        builder.append(" ");
        builder.append(actionButton("[INV]", ChatColor.YELLOW, "/tpa " + sender.getName(), "Inviter / demande TP"));
        builder.append(" ");
        builder.append(actionButton("[TP]", ChatColor.GOLD, "/tpa " + sender.getName(), "Teleporter vers ce joueur"));
        builder.append(" ");
        builder.append(":" + " ").color(ChatColor.WHITE);

        for (BaseComponent part : TextComponent.fromLegacyText(highlightMentions(formattedBody))) {
            builder.append(part);
        }

        builder.append(" ");
        builder.append(actionButton("[GG]", ChatColor.GREEN, "/react gg " + sender.getName(), "Reaction GG"));
        builder.append(" ");
        builder.append(actionButton("[LOL]", ChatColor.LIGHT_PURPLE, "/react lol " + sender.getName(), "Reaction LOL"));
        builder.append(" ");
        builder.append(actionButton("[+1]", ChatColor.YELLOW, "/react +1 " + sender.getName(), "Reaction +1"));

        return builder.create();
    }

    private BaseComponent[] buildPrivateMessage(boolean outgoing, Player from, Player to, String formattedBody) {
        String prefix = outgoing ? "[MP -> " + to.getName() + "] " : "[MP <- " + from.getName() + "] ";
        ComponentBuilder builder = new ComponentBuilder(prefix).color(ChatColor.LIGHT_PURPLE);

        for (BaseComponent part : TextComponent.fromLegacyText(highlightMentions(formattedBody))) {
            builder.append(part);
        }

        return builder.create();
    }

    private TextComponent actionButton(String label, ChatColor color, String command, String hover) {
        TextComponent button = new TextComponent(label);
        button.setColor(color);
        button.setBold(true);
        button.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command));
        button.setHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(hover).color(ChatColor.GRAY).create()
        ));
        return button;
    }

    private Set<Player> detectMentionedPlayers(String rawMessage) {
        Set<Player> mentioned = new HashSet<>();
        for (String token : rawMessage.split("\\s+")) {
            if (!token.startsWith("@") || token.length() <= 1) {
                continue;
            }
            String candidate = stripPunctuation(token.substring(1));
            Player player = Bukkit.getPlayerExact(candidate);
            if (player != null) {
                mentioned.add(player);
            }
        }
        return mentioned;
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

    private String applyMarkdownLikeFormatting(String input) {
        String output = input;
        output = output.replaceAll("\\*\\*(.+?)\\*\\*", org.bukkit.ChatColor.BOLD + "$1" + org.bukkit.ChatColor.RESET);
        output = output.replaceAll("\\*(.+?)\\*", org.bukkit.ChatColor.ITALIC + "$1" + org.bukkit.ChatColor.RESET);
        output = output.replaceAll("~~(.+?)~~", org.bukkit.ChatColor.STRIKETHROUGH + "$1" + org.bukkit.ChatColor.RESET);
        output = output.replaceAll("`(.+?)`", org.bukkit.ChatColor.GRAY + "$1" + org.bukkit.ChatColor.RESET);
        return output;
    }

    private List<String> onlinePlayerNamesExcluding(CommandSender sender) {
        UUID self = sender instanceof Player player ? player.getUniqueId() : null;
        List<String> names = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (self != null && self.equals(online.getUniqueId())) {
                continue;
            }
            names.add(online.getName());
        }
        return names;
    }

    private List<String> filterByPrefix(List<String> candidates, String partial) {
        String normalized = partial.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String candidate : candidates) {
            if (candidate.toLowerCase(Locale.ROOT).startsWith(normalized)) {
                matches.add(candidate);
            }
        }
        return matches;
    }

    private String[] slice(String[] source, int fromIndex) {
        if (fromIndex >= source.length) {
            return new String[0];
        }
        String[] result = new String[source.length - fromIndex];
        System.arraycopy(source, fromIndex, result, 0, result.length);
        return result;
    }

    private String stripPunctuation(String input) {
        return input.replaceAll("[^A-Za-z0-9_]", "");
    }
}
