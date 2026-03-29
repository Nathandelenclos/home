package fr.nathandelenclos.home;

import fr.nathandelenclos.home.application.HomeService;
import fr.nathandelenclos.home.application.TpaService;
import fr.nathandelenclos.home.application.WarpService;
import fr.nathandelenclos.home.domain.TeleportPoint;
import fr.nathandelenclos.home.infrastructure.BukkitLocationMapper;
import fr.nathandelenclos.home.infrastructure.InMemoryTpaRequestRepository;
import fr.nathandelenclos.home.infrastructure.YamlTeleportRepository;
import fr.nathandelenclos.home.presentation.ChatFeatureHandler;
import fr.nathandelenclos.home.presentation.TeleportCommandHandler;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.logging.Level;

public final class Home extends JavaPlugin {

    private static final List<String> TELEPORT_COMMAND_NAMES = List.of(
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

    private static final List<String> CHAT_COMMAND_NAMES = List.of(
            "msg",
            "r",
            "react",
            "playercard",
            "sharecoord",
            "coordtpall"
    );

    private TeleportCommandHandler commandHandler;
    private ChatFeatureHandler chatHandler;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        ConfigurationSerialization.registerClass(TeleportPoint.class);

        commandHandler = createTeleportCommandHandler();
        chatHandler = new ChatFeatureHandler(this);

        registerAllCommands();
        getServer().getPluginManager().registerEvents(chatHandler, this);
        getLogger().info("Home plugin active: architecture hexagonale chargee.");
    }

    private TeleportCommandHandler createTeleportCommandHandler() {
        YamlTeleportRepository repository = new YamlTeleportRepository(getConfig(), this::saveConfig);
        HomeService homeService = new HomeService(repository);
        WarpService warpService = new WarpService(repository);
        TpaService tpaService = new TpaService(new InMemoryTpaRequestRepository());
        BukkitLocationMapper locationMapper = new BukkitLocationMapper();
        return new TeleportCommandHandler(homeService, warpService, tpaService, locationMapper);
    }

    private void registerAllCommands() {
        registerCommands(TELEPORT_COMMAND_NAMES, commandHandler);
        registerCommands(CHAT_COMMAND_NAMES, chatHandler);
    }

    @Override
    public void onDisable() {
        saveConfig();
    }

    private void registerCommands(List<String> commandNames, TabExecutor executor) {
        for (String name : commandNames) {
            registerCommand(name, executor);
        }
    }

    private void registerCommand(String name, TabExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            if (getLogger().isLoggable(Level.WARNING)) {
                getLogger().warning("Commande manquante dans plugin.yml: " + name);
            }
            return;
        }
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }
}
