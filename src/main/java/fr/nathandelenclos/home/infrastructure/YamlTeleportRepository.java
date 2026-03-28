package fr.nathandelenclos.home.infrastructure;

import fr.nathandelenclos.home.application.port.HomeRepository;
import fr.nathandelenclos.home.application.port.WarpRepository;
import fr.nathandelenclos.home.domain.TeleportPoint;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

public final class YamlTeleportRepository implements HomeRepository, WarpRepository {

    private static final String HOMES_ROOT = "homes";
    private static final String WARPS_ROOT = "warps";

    private final FileConfiguration config;
    private final Runnable saveAction;

    public YamlTeleportRepository(FileConfiguration config, Runnable saveAction) {
        this.config = config;
        this.saveAction = saveAction;
    }

    @Override
    public void save(UUID playerId, String homeName, TeleportPoint point) {
        config.set(homePath(playerId, homeName), point);
        saveAction.run();
    }

    @Override
    public Optional<TeleportPoint> find(UUID playerId, String homeName) {
        String path = homePath(playerId, homeName);
        TeleportPoint direct = config.getSerializable(path, TeleportPoint.class);
        if (direct != null) {
            return Optional.of(direct);
        }

        Location legacy = config.getLocation(path);
        if (legacy == null) {
            return Optional.empty();
        }

        TeleportPoint migrated = TeleportPoint.fromLocation(legacy);
        config.set(path, migrated);
        saveAction.run();
        return Optional.of(migrated);
    }

    @Override
    public List<String> listNames(UUID playerId) {
        ConfigurationSection section = config.getConfigurationSection(HOMES_ROOT + "." + playerId);
        if (section == null) {
            return Collections.emptyList();
        }
        return sortedNames(section.getKeys(false));
    }

    @Override
    public boolean delete(UUID playerId, String homeName) {
        String path = homePath(playerId, homeName);
        if (!config.contains(path)) {
            return false;
        }
        config.set(path, null);
        saveAction.run();
        return true;
    }

    @Override
    public void save(String warpName, TeleportPoint point) {
        config.set(warpPath(warpName), point);
        saveAction.run();
    }

    @Override
    public Optional<TeleportPoint> find(String warpName) {
        String path = warpPath(warpName);
        TeleportPoint direct = config.getSerializable(path, TeleportPoint.class);
        if (direct != null) {
            return Optional.of(direct);
        }

        Location legacy = config.getLocation(path);
        if (legacy == null) {
            return Optional.empty();
        }

        TeleportPoint migrated = TeleportPoint.fromLocation(legacy);
        config.set(path, migrated);
        saveAction.run();
        return Optional.of(migrated);
    }

    @Override
    public List<String> listNames() {
        ConfigurationSection section = config.getConfigurationSection(WARPS_ROOT);
        if (section == null) {
            return Collections.emptyList();
        }
        return sortedNames(section.getKeys(false));
    }

    @Override
    public boolean delete(String warpName) {
        String path = warpPath(warpName);
        if (!config.contains(path)) {
            return false;
        }
        config.set(path, null);
        saveAction.run();
        return true;
    }

    private String homePath(UUID playerId, String homeName) {
        return HOMES_ROOT + "." + playerId + "." + homeName;
    }

    private String warpPath(String warpName) {
        return WARPS_ROOT + "." + warpName;
    }

    private List<String> sortedNames(Set<String> names) {
        Set<String> sorted = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        sorted.addAll(names);
        return new ArrayList<>(sorted);
    }
}
