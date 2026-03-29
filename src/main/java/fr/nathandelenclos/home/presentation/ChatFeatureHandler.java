package fr.nathandelenclos.home.presentation;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
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

    private static final String PLAYER_CARD_PREFIX = "Profil: ";
    private static final int SLOT_INFO = 11;
    private static final int SLOT_TPA = 13;
    private static final int SLOT_MP = 15;
    private static final int SLOT_REACT_GG = 21;
    private static final int SLOT_REACT_LOL = 22;
    private static final int SLOT_REACT_FIRE = 23;

    private final Plugin plugin;
    private final Map<UUID, UUID> lastPrivateContactByPlayer = new HashMap<>();
    private final Map<UUID, UUID> profileTargetByViewer = new HashMap<>();

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

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player viewer)) {
            return;
        }

        String title = event.getView().getTitle();
        if (!title.startsWith(PLAYER_CARD_PREFIX)) {
            return;
        }

        event.setCancelled(true);
        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getInventory().getSize()) {
            return;
        }

        UUID targetId = profileTargetByViewer.get(viewer.getUniqueId());
        if (targetId == null) {
            viewer.closeInventory();
            return;
        }

        Player target = Bukkit.getPlayer(targetId);
        if (target == null) {
            viewer.sendMessage(org.bukkit.ChatColor.RED + "Le joueur n'est plus en ligne.");
            viewer.closeInventory();
            return;
        }

        switch (event.getRawSlot()) {
            case SLOT_TPA -> {
                viewer.closeInventory();
                viewer.performCommand("tpa " + target.getName());
            }
            case SLOT_MP -> {
                viewer.closeInventory();
                TextComponent hint = new TextComponent(org.bukkit.ChatColor.AQUA
                        + "Clique ici pour pre-remplir ton MP vers " + target.getName());
                hint.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/msg " + target.getName() + " "));
                hint.setHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        new ComponentBuilder("Pre-remplit la commande /msg").color(ChatColor.GRAY).create()
                ));
                viewer.spigot().sendMessage(hint);
            }
            case SLOT_REACT_GG -> {
                viewer.closeInventory();
                viewer.performCommand("react gg " + target.getName());
            }
            case SLOT_REACT_LOL -> {
                viewer.closeInventory();
                viewer.performCommand("react lol " + target.getName());
            }
            case SLOT_REACT_FIRE -> {
                viewer.closeInventory();
                viewer.performCommand("react fire " + target.getName());
            }
            default -> {
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().startsWith(PLAYER_CARD_PREFIX)
                && event.getPlayer() instanceof Player player) {
            profileTargetByViewer.remove(player.getUniqueId());
        }
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

        openPlayerCard(viewer, target);
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
                        .color(ChatColor.GRAY)
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
        player.sendMessage(org.bukkit.ChatColor.GREEN + "Teleportation vers "
            + worldName + " " + x + " " + y + " " + z + ".");
        return true;
    }

    private void openPlayerCard(Player viewer, Player target) {
        Inventory inventory = Bukkit.createInventory(viewer, 27, PLAYER_CARD_PREFIX + target.getName());

        inventory.setItem(SLOT_INFO, makeItem(
                Material.PLAYER_HEAD,
                org.bukkit.ChatColor.AQUA + "Infos: " + target.getName(),
                List.of(
                        org.bukkit.ChatColor.GRAY + "Monde: " + target.getWorld().getName(),
                        org.bukkit.ChatColor.GRAY + "X: " + (int) target.getLocation().getX()
                                + " Y: " + (int) target.getLocation().getY()
                                + " Z: " + (int) target.getLocation().getZ()
                )
        ));
        inventory.setItem(SLOT_TPA, makeItem(
                Material.ENDER_PEARL,
                org.bukkit.ChatColor.GREEN + "Demande TP",
                List.of(org.bukkit.ChatColor.GRAY + "Envoie /tpa " + target.getName())
        ));
        inventory.setItem(SLOT_MP, makeItem(
                Material.WRITABLE_BOOK,
                org.bukkit.ChatColor.LIGHT_PURPLE + "Message prive",
                List.of(org.bukkit.ChatColor.GRAY + "Pre-remplit /msg " + target.getName())
        ));
        inventory.setItem(SLOT_REACT_GG, makeItem(
                Material.EMERALD,
                org.bukkit.ChatColor.GREEN + "Reaction GG",
                List.of(org.bukkit.ChatColor.GRAY + "Envoie /react gg " + target.getName())
        ));
        inventory.setItem(SLOT_REACT_LOL, makeItem(
                Material.HONEY_BOTTLE,
                org.bukkit.ChatColor.GOLD + "Reaction LOL",
                List.of(org.bukkit.ChatColor.GRAY + "Envoie /react lol " + target.getName())
        ));
        inventory.setItem(SLOT_REACT_FIRE, makeItem(
                Material.BLAZE_POWDER,
                org.bukkit.ChatColor.RED + "Reaction FIRE",
                List.of(org.bukkit.ChatColor.GRAY + "Envoie /react fire " + target.getName())
        ));

        profileTargetByViewer.put(viewer.getUniqueId(), target.getUniqueId());
        viewer.openInventory(inventory);
    }

    private ItemStack makeItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
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
                new ComponentBuilder("Heure exacte: " + nowLong + "\nClic gauche: pre-remplir /msg\nProfil: /playercard " + sender.getName())
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

    private BaseComponent[] buildPrivateMessage(boolean outgoing, Player from, Player to, String formattedBody) {
        String prefix = outgoing ? "[MP -> " + to.getName() + "] " : "[MP <- " + from.getName() + "] ";
        ComponentBuilder builder = new ComponentBuilder(prefix).color(ChatColor.LIGHT_PURPLE);

        for (BaseComponent part : TextComponent.fromLegacyText(highlightMentions(formattedBody))) {
            builder.append(part);
        }

        return builder.create();
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

    private String stripPunctuation(String input) {
        return input.replaceAll("[^A-Za-z0-9_]", "");
    }
}
