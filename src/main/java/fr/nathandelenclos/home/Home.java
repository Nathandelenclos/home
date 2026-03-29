package fr.nathandelenclos.home;

import fr.nathandelenclos.home.application.HomeService;
import fr.nathandelenclos.home.application.TpaService;
import fr.nathandelenclos.home.application.WarpService;
import fr.nathandelenclos.home.domain.TeleportPoint;
import fr.nathandelenclos.home.infrastructure.BukkitLocationMapper;
import fr.nathandelenclos.home.infrastructure.InMemoryTpaRequestRepository;
import fr.nathandelenclos.home.infrastructure.YamlTeleportRepository;
import fr.nathandelenclos.home.presentation.TeleportCommandHandler;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.logging.Level;

public final class Home extends JavaPlugin {

    private static final List<String> COMMAND_NAMES = List.of(
            "sethome",
            "home",
            "delhome",
            "setwarp",
            "warp",
            "delwarp",
            "warps",
            "tpa",
            "tpaccept",
            "tpdeny"
    );

    private TeleportCommandHandler commandHandler;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        ConfigurationSerialization.registerClass(TeleportPoint.class);

        YamlTeleportRepository repository = new YamlTeleportRepository(getConfig(), this::saveConfig);
        HomeService homeService = new HomeService(repository);
        WarpService warpService = new WarpService(repository);
        TpaService tpaService = new TpaService(new InMemoryTpaRequestRepository());
        BukkitLocationMapper locationMapper = new BukkitLocationMapper();
        commandHandler = new TeleportCommandHandler(homeService, warpService, tpaService, locationMapper);

        registerCommands();
        getLogger().info("Home plugin active: architecture hexagonale chargee.");
    }

    @Override
    public void onDisable() {
        saveConfig();
    }

    private void registerCommands() {
        for (String name : COMMAND_NAMES) {
            registerCommand(name);
        }
    }

    private void registerCommand(String name) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            if (getLogger().isLoggable(Level.WARNING)) {
                getLogger().warning("Commande manquante dans plugin.yml: " + name);
            }
            return;
        }
        command.setExecutor(commandHandler);
        command.setTabCompleter(commandHandler);
    }
}
