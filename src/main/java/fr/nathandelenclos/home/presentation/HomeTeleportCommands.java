package fr.nathandelenclos.home.presentation;

import fr.nathandelenclos.home.application.HomeService;
import fr.nathandelenclos.home.domain.TeleportName;
import fr.nathandelenclos.home.domain.TeleportPoint;
import fr.nathandelenclos.home.infrastructure.BukkitLocationMapper;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

final class HomeTeleportCommands {

    private final HomeService homeService;
    private final BukkitLocationMapper locationMapper;

    HomeTeleportCommands(HomeService homeService, BukkitLocationMapper locationMapper) {
        this.homeService = homeService;
        this.locationMapper = locationMapper;
    }

    boolean setHome(Player player, String homeNameRaw) {
        TeleportPoint point = locationMapper.toDomain(player.getLocation());
        String homeName = homeService.setHome(player.getUniqueId(), homeNameRaw, point);
        player.sendMessage(CommandMessages.homeSaved(homeName));
        return true;
    }

    boolean home(Player player, String[] args) {
        if (args.length == 0) {
            List<String> homes = homeService.listHomes(player.getUniqueId());
            if (homes.isEmpty()) {
                player.sendMessage(CommandMessages.HOMES_EMPTY);
                return true;
            }
            player.sendMessage(CommandMessages.homesList(String.join(", ", homes)));
            return true;
        }

        if (args.length != 1) {
            return false;
        }

        Optional<TeleportPoint> maybeHome = homeService.findHome(player.getUniqueId(), args[0]);
        if (maybeHome.isEmpty()) {
            player.sendMessage(CommandMessages.homeNotFound(args[0]));
            return true;
        }

        Optional<org.bukkit.Location> maybeLocation = locationMapper.toBukkit(maybeHome.get());
        if (maybeLocation.isEmpty()) {
            player.sendMessage(CommandMessages.HOME_WORLD_MISSING);
            return true;
        }

        player.teleport(maybeLocation.get());
        player.sendMessage(CommandMessages.homeTeleported(normalized(args[0])));
        return true;
    }

    boolean deleteHome(Player player, String homeNameRaw) {
        boolean deleted = homeService.deleteHome(player.getUniqueId(), homeNameRaw);
        if (!deleted) {
            player.sendMessage(CommandMessages.homeNotFound(homeNameRaw));
            return true;
        }
        player.sendMessage(CommandMessages.homeDeleted(normalized(homeNameRaw)));
        return true;
    }

    List<String> completions(Player player) {
        return homeService.listHomes(player.getUniqueId());
    }

    private String normalized(String rawName) {
        return TeleportName.fromRaw(rawName).value();
    }
}
