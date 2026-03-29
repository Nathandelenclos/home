package fr.nathandelenclos.home.presentation;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class PlayerCardGui {

    private static final String PLAYER_CARD_PREFIX = "Profil: ";
    private static final int SLOT_INFO = 11;
    private static final int SLOT_TPA = 13;
    private static final int SLOT_MP = 15;
    private static final int SLOT_REACT_GG = 21;
    private static final int SLOT_REACT_LOL = 22;
    private static final int SLOT_REACT_FIRE = 23;

    private final Map<UUID, UUID> profileTargetByViewer = new HashMap<>();

    void open(Player viewer, Player target) {
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

    void onInventoryClick(InventoryClickEvent event) {
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

    void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().startsWith(PLAYER_CARD_PREFIX)
                && event.getPlayer() instanceof Player player) {
            profileTargetByViewer.remove(player.getUniqueId());
        }
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
}
