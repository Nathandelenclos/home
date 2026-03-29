package fr.nathandelenclos.home.presentation;

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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ChatFeatureHandler implements Listener, TabExecutor {

    private static final Set<String> ALLOWED_REACTIONS = Set.of("gg", "lol", "+1", "rip", "fire");

    private final Plugin plugin;
    private final ChatRenderer renderer;
    private final PlayerCardGui playerCardGui;
    private final Map<UUID, UUID> lastPrivateContactByPlayer = new HashMap<>();

    private volatile UUID lastPublicMessageSender;

    public ChatFeatureHandler(Plugin plugin) {
        this.plugin = plugin;
        this.renderer = new ChatRenderer();
        this.playerCardGui = new PlayerCardGui();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer();
        String rawMessage = event.getMessage();
        String formatted = renderer.formatMarkdownLike(rawMessage);

        event.setCancelled(true);

        Bukkit.getScheduler().runTask(plugin, () -> {
            List<Player> recipients = new ArrayList<>(event.getRecipients());
            if (!recipients.contains(sender)) {
                recipients.add(sender);
            }

            BaseComponent[] message = renderer.buildPublicMessage(sender, formatted);
            for (Player recipient : recipients) {
                recipient.spigot().sendMessage(message);
            }

            for (Player mentioned : renderer.detectMentionedPlayers(rawMessage)) {
                mentioned.playSound(mentioned.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.4f);
            }

            lastPublicMessageSender = sender.getUniqueId();
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        playerCardGui.onInventoryClick(event);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        playerCardGui.onInventoryClose(event);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        return switch (name) {
            case "msg" -> handleMsg(sender, args);
            case "r" -> handleReply(sender, args);
            case "react" -> handleReact(sender, args);
            case "playercard" -> handlePlayerCard(sender, args);
            case "sharecoord" -> handleShareCoord(sender, args);
            case "coordtpall" -> handleCoordTpAll(sender, args);
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
        if ("playercard".equals(name) && args.length == 1) {
            return filterByPrefix(onlinePlayerNames(), args[0]);
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
        String formatted = renderer.formatMarkdownLike(message);

        from.spigot().sendMessage(renderer.buildPrivateMessage(true, from, to, formatted));
        to.spigot().sendMessage(renderer.buildPrivateMessage(false, from, to, formatted));

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
        String formatted = renderer.formatMarkdownLike(message);

        from.spigot().sendMessage(renderer.buildPrivateMessage(true, from, to, formatted));
        to.spigot().sendMessage(renderer.buildPrivateMessage(false, from, to, formatted));

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
            from.sendMessage(org.bukkit.ChatColor.RED
                    + "Reaction invalide. Disponibles: " + String.join(", ", ALLOWED_REACTIONS));
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

    private boolean handlePlayerCard(CommandSender sender, String[] args) {
        if (!(sender instanceof Player viewer)) {
            sender.sendMessage(org.bukkit.ChatColor.RED + "Commande reservee aux joueurs.");
            return true;
        }
        if (args.length != 1) {
            return false;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            viewer.sendMessage(org.bukkit.ChatColor.RED + "Joueur introuvable: " + args[0]);
            return true;
        }

        playerCardGui.open(viewer, target);
        return true;
    }

    private boolean handleShareCoord(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(org.bukkit.ChatColor.RED + "Commande reservee aux joueurs.");
            return true;
        }
        if (args.length != 0) {
            return false;
        }

        String world = player.getWorld().getName();
        int x = player.getLocation().getBlockX();
        int y = player.getLocation().getBlockY();
        int z = player.getLocation().getBlockZ();

        String clickCommand = "/coordtpall " + world + " " + x + " " + y + " " + z;
        TextComponent line = new TextComponent(
                org.bukkit.ChatColor.GOLD + "[Coord] "
                        + org.bukkit.ChatColor.AQUA + player.getName()
                        + org.bukkit.ChatColor.WHITE + " a partage: "
                        + org.bukkit.ChatColor.YELLOW + world + " " + x + " " + y + " " + z
                        + org.bukkit.ChatColor.GREEN + " (cliquer pour te teleporter)"
        );
        line.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, clickCommand));
        line.setHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("Clique pour te teleporter a ces coordonnees")
                        .color(net.md_5.bungee.api.ChatColor.GRAY)
                        .create()
        ));

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.spigot().sendMessage(line);
        }
        return true;
    }

    private boolean handleCoordTpAll(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(org.bukkit.ChatColor.RED + "Commande reservee aux joueurs.");
            return true;
        }
        if (args.length != 4) {
            return false;
        }

        String worldName = args[0];
        org.bukkit.World world = Bukkit.getWorld(worldName);
        if (world == null) {
            player.sendMessage(org.bukkit.ChatColor.RED + "Monde introuvable: " + worldName);
            return true;
        }

        int x;
        int y;
        int z;
        try {
            x = Integer.parseInt(args[1]);
            y = Integer.parseInt(args[2]);
            z = Integer.parseInt(args[3]);
        } catch (NumberFormatException ex) {
            player.sendMessage(org.bukkit.ChatColor.RED + "Coordonnees invalides.");
            return true;
        }

        org.bukkit.Location destination = new org.bukkit.Location(world, x + 0.5, y, z + 0.5);
        player.teleport(destination);
        player.sendMessage(org.bukkit.ChatColor.GREEN + "Teleportation vers " + worldName + " " + x + " " + y + " " + z + ".");
        return true;
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

    private List<String> onlinePlayerNames() {
        List<String> names = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
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
}
