package fr.nathandelenclos.home.domain;

import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@SerializableAs("TeleportPoint")
public record TeleportPoint(String worldName, double x, double y, double z, float yaw, float pitch)
    implements ConfigurationSerializable {

    public static TeleportPoint fromLocation(Location location) {
        Objects.requireNonNull(location, "location");
        if (location.getWorld() == null) {
            throw new IllegalArgumentException("Location world cannot be null");
        }
        return new TeleportPoint(
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch()
        );
    }

    public Map<String, Object> serialize() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("worldName", worldName);
        data.put("x", x);
        data.put("y", y);
        data.put("z", z);
        data.put("yaw", yaw);
        data.put("pitch", pitch);
        return data;
    }

    public static TeleportPoint deserialize(Map<String, Object> data) {
        String worldName = Objects.toString(data.get("worldName"), null);
        double x = toDouble(data.get("x"));
        double y = toDouble(data.get("y"));
        double z = toDouble(data.get("z"));
        float yaw = (float) toDouble(data.get("yaw"));
        float pitch = (float) toDouble(data.get("pitch"));
        if (worldName == null || worldName.isBlank()) {
            throw new IllegalArgumentException("worldName cannot be null or blank");
        }
        return new TeleportPoint(worldName, x, y, z, yaw, pitch);
    }

    private static double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        throw new IllegalArgumentException("Expected number value");
    }
}
