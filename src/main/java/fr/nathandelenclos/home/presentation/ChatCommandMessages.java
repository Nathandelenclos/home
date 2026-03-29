package fr.nathandelenclos.home.presentation;

import org.bukkit.ChatColor;

import java.util.List;

final class ChatCommandMessages {

    static final String PLAYER_ONLY = ChatColor.RED + "Commande reservee aux joueurs.";
    static final String NO_REPLY_TARGET = ChatColor.YELLOW + "Aucun dernier contact pour repondre.";
    static final String REPLY_TARGET_OFFLINE = ChatColor.RED + "Ton dernier contact n'est plus en ligne.";
    static final String NO_RECENT_PUBLIC_MESSAGE = ChatColor.YELLOW + "Aucun message recent pour reagir.";
    static final String REACTION_TARGET_OFFLINE = ChatColor.YELLOW + "Le joueur cible n'est plus en ligne.";
    static final String INVALID_COORDINATES = ChatColor.RED + "Coordonnees invalides.";
    static final String SHARED_COORDINATES_HOVER = "Clique pour te teleporter a ces coordonnees";

    private ChatCommandMessages() {
    }

    static String playerNotFound(String playerName) {
        return ChatColor.RED + "Joueur introuvable: " + playerName;
    }

    static String selfMessageForbidden() {
        return ChatColor.RED + "Tu ne peux pas t'envoyer un message a toi-meme.";
    }

    static String invalidReaction(List<String> allowedReactions) {
        return ChatColor.RED + "Reaction invalide. Disponibles: " + String.join(", ", allowedReactions);
    }

    static String worldNotFound(String worldName) {
        return ChatColor.RED + "Monde introuvable: " + worldName;
    }

    static String teleportedToCoordinates(String worldName, int x, int y, int z) {
        return ChatColor.GREEN + "Teleportation vers " + worldName + " " + x + " " + y + " " + z + ".";
    }

    static String reactionBroadcast(String fromName, String targetName, String displayReaction) {
        return ChatColor.GOLD + "[Reaction] " + ChatColor.YELLOW + fromName
                + ChatColor.WHITE + " -> " + ChatColor.AQUA + targetName
                + ChatColor.WHITE + " : " + ChatColor.GOLD + displayReaction;
    }

    static String sharedCoordinatesLine(String senderName, String worldName, int x, int y, int z) {
        return ChatColor.GOLD + "[Coord] "
                + ChatColor.AQUA + senderName
                + ChatColor.WHITE + " a partage: "
                + ChatColor.YELLOW + worldName + " " + x + " " + y + " " + z
                + ChatColor.GREEN + " (cliquer pour te teleporter)";
    }
}
