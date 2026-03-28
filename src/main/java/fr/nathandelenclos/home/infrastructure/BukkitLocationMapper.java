package fr.nathandelenclos.home.infrastructure;

import fr.nathandelenclos.home.domain.TeleportPoint;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Optional;

public final class BukkitLocationMapper {

    public TeleportPoint toDomain(Location location) {
        return TeleportPoint.fromLocation(location);
    }

    public Optional<Location> toBukkit(TeleportPoint point) {
        World world = Bukkit.getWorld(point.worldName());
        if (world == null) {
            return Optional.empty();
        }
        return Optional.of(new Location(
                world,
                point.x(),
                point.y(),
                point.z(),
                point.yaw(),
                point.pitch()
        ));
    }
}
