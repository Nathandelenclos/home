package fr.nathandelenclos.home.presentation;

import org.bukkit.ChatColor;

final class CommandMessages {

    static final String PLAYER_ONLY = ChatColor.RED + "Cette commande est reservee aux joueurs.";
    static final String INVALID_NAME = ChatColor.RED + "Nom invalide.";

    private CommandMessages() {
    }

    static String homeSaved(String name) {
        return ChatColor.GREEN + "Home '" + name + "' enregistre.";
    }

    static String homeDeleted(String name) {
        return ChatColor.GREEN + "Home supprime: " + name;
    }

    static String homeNotFound(String name) {
        return ChatColor.RED + "Home introuvable: " + name;
    }

    static String homeTeleported(String name) {
        return ChatColor.GREEN + "Teleportation vers le home '" + name + "'.";
    }

    static String homesList(String names) {
        return ChatColor.AQUA + "Tes homes: " + ChatColor.WHITE + names;
    }

    static final String HOMES_EMPTY = ChatColor.YELLOW + "Tu n'as aucun home. Utilise /sethome <nom>.";

    static String warpSaved(String name) {
        return ChatColor.GREEN + "Warp '" + name + "' enregistre.";
    }

    static String warpDeleted(String name) {
        return ChatColor.GREEN + "Warp supprime: " + name;
    }

    static String warpNotFound(String name) {
        return ChatColor.RED + "Warp introuvable: " + name;
    }

    static String warpTeleported(String name) {
        return ChatColor.GREEN + "Teleportation vers le warp '" + name + "'.";
    }

    static String warpsList(String names) {
        return ChatColor.AQUA + "Warps disponibles: " + ChatColor.WHITE + names;
    }

    static final String WARPS_EMPTY = ChatColor.YELLOW + "Aucun warp disponible. Utilise /setwarp <nom>.";
    static final String HOME_WORLD_MISSING = ChatColor.RED + "Le monde de ce home n'existe plus.";
    static final String WARP_WORLD_MISSING = ChatColor.RED + "Le monde de ce warp n'existe plus.";
}
