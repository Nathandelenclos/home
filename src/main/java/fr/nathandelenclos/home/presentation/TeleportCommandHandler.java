package fr.nathandelenclos.home.presentation;

import fr.nathandelenclos.home.application.HomeService;
import fr.nathandelenclos.home.application.TpaService;
import fr.nathandelenclos.home.application.WarpService;
import fr.nathandelenclos.home.infrastructure.BukkitLocationMapper;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class TeleportCommandHandler implements TabExecutor {

    @FunctionalInterface
    private interface CommandAction {
        boolean execute(CommandSender sender, String[] args);
    }

    @FunctionalInterface
    private interface PlayerCommandAction {
        boolean execute(Player player, String[] args);
    }

    @FunctionalInterface
    private interface ValidatedAction {
        boolean execute();
    }

    private final HomeTeleportCommands homeCommands;
    private final WarpTeleportCommands warpCommands;
    private final TpaTeleportCommands tpaCommands;
    private final Map<String, CommandAction> commandRouter;

    public TeleportCommandHandler(
            HomeService homeService,
            WarpService warpService,
            TpaService tpaService,
            BukkitLocationMapper locationMapper
    ) {
        this.homeCommands = new HomeTeleportCommands(homeService, locationMapper);
        this.warpCommands = new WarpTeleportCommands(warpService, locationMapper);
        this.tpaCommands = new TpaTeleportCommands(tpaService);
        this.commandRouter = buildRouter();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String commandName = command.getName().toLowerCase(Locale.ROOT);
        CommandAction action = commandRouter.get(commandName);
        return action != null && action.execute(sender, args);
    }

    private Map<String, CommandAction> buildRouter() {
        Map<String, CommandAction> routes = new LinkedHashMap<>();
        routes.put("sethome", this::handleSetHome);
        routes.put("home", this::handleHome);
        routes.put("delhome", this::handleDelHome);
        routes.put("setwarp", this::handleSetWarp);
        routes.put("warp", this::handleWarp);
        routes.put("delwarp", this::handleDelWarp);
        routes.put("warps", this::handleWarps);
        routes.put("tpa", this::handleTpa);
        routes.put("tpaccept", this::handleTpAccept);
        routes.put("tpdeny", this::handleTpDeny);
        return Collections.unmodifiableMap(routes);
    }

    private Optional<Player> getPlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return Optional.of(player);
        }
        sender.sendMessage(CommandMessages.PLAYER_ONLY);
        return Optional.empty();
    }

    private boolean withPlayerAndExactArgs(
            CommandSender sender,
            String[] args,
            int expectedArgs,
            PlayerCommandAction action
    ) {
        Optional<Player> maybePlayer = getPlayer(sender);
        if (maybePlayer.isEmpty()) {
            return true;
        }
        if (args.length != expectedArgs) {
            return false;
        }
        return action.execute(maybePlayer.get(), args);
    }

    private boolean withInvalidNameGuard(Player player, ValidatedAction action) {
        try {
            return action.execute();
        } catch (IllegalArgumentException ex) {
            player.sendMessage(CommandMessages.INVALID_NAME);
            return true;
        }
    }

    private boolean handleSetHome(CommandSender sender, String[] args) {
        return withPlayerAndExactArgs(sender, args, 1,
                (player, actualArgs) -> withInvalidNameGuard(player, () -> homeCommands.setHome(player, actualArgs[0])));
    }

    private boolean handleHome(CommandSender sender, String[] args) {
        Optional<Player> maybePlayer = getPlayer(sender);
        if (maybePlayer.isEmpty()) {
            return true;
        }
        Player player = maybePlayer.get();
        return withInvalidNameGuard(player, () -> homeCommands.home(player, args));
    }

    private boolean handleSetWarp(CommandSender sender, String[] args) {
        return withPlayerAndExactArgs(sender, args, 1,
                (player, actualArgs) -> withInvalidNameGuard(player, () -> warpCommands.setWarp(player, actualArgs[0])));
    }

    private boolean handleDelHome(CommandSender sender, String[] args) {
        return withPlayerAndExactArgs(sender, args, 1,
                (player, actualArgs) -> withInvalidNameGuard(player, () -> homeCommands.deleteHome(player, actualArgs[0])));
    }

    private boolean handleWarp(CommandSender sender, String[] args) {
        return withPlayerAndExactArgs(sender, args, 1,
                (player, actualArgs) -> withInvalidNameGuard(player, () -> warpCommands.warp(player, actualArgs[0])));
    }

    private boolean handleWarps(CommandSender sender, String[] args) {
        return warpCommands.warps(sender, args);
    }

    private boolean handleDelWarp(CommandSender sender, String[] args) {
        return withPlayerAndExactArgs(sender, args, 1,
                (player, actualArgs) -> withInvalidNameGuard(player, () -> warpCommands.deleteWarp(player, actualArgs[0])));
    }

    private boolean handleTpa(CommandSender sender, String[] args) {
        return withPlayerAndExactArgs(sender, args, 1, (requester, actualArgs) -> tpaCommands.tpa(requester, actualArgs[0]));
    }

    private boolean handleTpAccept(CommandSender sender, String[] args) {
        return withPlayerAndExactArgs(sender, args, 0, (target, actualArgs) -> tpaCommands.accept(target));
    }

    private boolean handleTpDeny(CommandSender sender, String[] args) {
        return withPlayerAndExactArgs(sender, args, 0, (target, actualArgs) -> tpaCommands.deny(target));
    }

    private List<String> filterByPrefix(List<String> candidates, String partial) {
        List<String> matches = new ArrayList<>();
        for (String candidate : candidates) {
            if (candidate.toLowerCase(Locale.ROOT).startsWith(partial)) {
                matches.add(candidate);
            }
        }
        return matches;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }

        String partial = args[0].toLowerCase(Locale.ROOT);
        String commandName = command.getName().toLowerCase(Locale.ROOT);
        List<String> candidates;

        if (("home".equals(commandName) || "delhome".equals(commandName)) && sender instanceof Player player) {
            candidates = homeCommands.completions(player);
        } else if ("warp".equals(commandName) || "delwarp".equals(commandName)) {
            candidates = warpCommands.completions();
        } else if ("tpa".equals(commandName) && sender instanceof Player player) {
            candidates = tpaCommands.completions(player);
        } else {
            return Collections.emptyList();
        }

        return filterByPrefix(candidates, partial);
    }
}
