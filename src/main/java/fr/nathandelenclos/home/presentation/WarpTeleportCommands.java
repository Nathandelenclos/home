package fr.nathandelenclos.home.presentation;

import fr.nathandelenclos.home.application.WarpService;
import fr.nathandelenclos.home.domain.TeleportName;
import fr.nathandelenclos.home.domain.TeleportPoint;
import fr.nathandelenclos.home.infrastructure.BukkitLocationMapper;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

final class WarpTeleportCommands {

    private final WarpService warpService;
    private final BukkitLocationMapper locationMapper;

    WarpTeleportCommands(WarpService warpService, BukkitLocationMapper locationMapper) {
        this.warpService = warpService;
        this.locationMapper = locationMapper;
    }

    boolean setWarp(Player player, String warpNameRaw) {
        TeleportPoint point = locationMapper.toDomain(player.getLocation());
        String warpName = warpService.setWarp(warpNameRaw, point);
        player.sendMessage(CommandMessages.warpSaved(warpName));
        return true;
    }

    boolean warp(Player player, String warpNameRaw) {
        Optional<TeleportPoint> maybeWarp = warpService.findWarp(warpNameRaw);
        if (maybeWarp.isEmpty()) {
            player.sendMessage(CommandMessages.warpNotFound(warpNameRaw));
            return true;
        }

        Optional<org.bukkit.Location> maybeLocation = locationMapper.toBukkit(maybeWarp.get());
        if (maybeLocation.isEmpty()) {
            player.sendMessage(CommandMessages.WARP_WORLD_MISSING);
            return true;
        }

        player.teleport(maybeLocation.get());
        player.sendMessage(CommandMessages.warpTeleported(normalized(warpNameRaw)));
        return true;
    }

    boolean deleteWarp(Player player, String warpNameRaw) {
        boolean deleted = warpService.deleteWarp(warpNameRaw);
        if (!deleted) {
            player.sendMessage(CommandMessages.warpNotFound(warpNameRaw));
            return true;
        }
        player.sendMessage(CommandMessages.warpDeleted(normalized(warpNameRaw)));
        return true;
    }

    boolean warps(CommandSender sender, String[] args) {
        if (args.length != 0) {
            return false;
        }
        List<String> warps = warpService.listWarps();
        if (warps.isEmpty()) {
            sender.sendMessage(CommandMessages.WARPS_EMPTY);
            return true;
        }
        sender.sendMessage(CommandMessages.warpsList(String.join(", ", warps)));
        return true;
    }

    List<String> completions() {
        return warpService.listWarps();
    }

    private String normalized(String rawName) {
        return TeleportName.fromRaw(rawName).value();
    }
}
