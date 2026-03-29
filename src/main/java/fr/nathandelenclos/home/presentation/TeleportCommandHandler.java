package fr.nathandelenclos.home.presentation;

import fr.nathandelenclos.home.application.HomeService;
import fr.nathandelenclos.home.application.TpaService;
import fr.nathandelenclos.home.application.WarpService;
import fr.nathandelenclos.home.domain.TeleportName;
import fr.nathandelenclos.home.domain.TeleportPoint;
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
import java.util.function.Function;

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
    private interface GuardedAction {
        boolean execute();
    }

    @FunctionalInterface
    private interface TeleportPointLookup {
        Optional<TeleportPoint> find(String rawName);
    }

    private final HomeService homeService;
    private final WarpService warpService;
    private final TpaService tpaService;
    private final BukkitLocationMapper locationMapper;
    private final Map<String, CommandAction> commandRouter;

    public TeleportCommandHandler(
            HomeService homeService,
            WarpService warpService,
            TpaService tpaService,
            BukkitLocationMapper locationMapper
    ) {
        this.homeService = homeService;
        this.warpService = warpService;
        this.tpaService = tpaService;
        this.locationMapper = locationMapper;
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

    private boolean withInvalidNameGuard(Player player, GuardedAction action) {
        try {
            return action.execute();
        } catch (IllegalArgumentException ex) {
            player.sendMessage(CommandMessages.INVALID_NAME);
            return true;
        }
    }

    private String normalized(String rawName) {
        return TeleportName.fromRaw(rawName).value();
    }

    private boolean handleSetHome(CommandSender sender, String[] args) {
        return withPlayerAndExactArgs(sender, args, 1, (player, actualArgs) -> withInvalidNameGuard(player, () -> {
            TeleportPoint point = locationMapper.toDomain(player.getLocation());
            String homeName = homeService.setHome(player.getUniqueId(), actualArgs[0], point);
            player.sendMessage(CommandMessages.homeSaved(homeName));
            return true;
        }));
    }

    private boolean handleHome(CommandSender sender, String[] args) {
        Optional<Player> maybePlayer = getPlayer(sender);
        if (maybePlayer.isEmpty()) {
            return true;
        }
        Player player = maybePlayer.get();

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

        return withInvalidNameGuard(player, () -> executeTeleport(
                player,
                args[0],
                rawName -> homeService.findHome(player.getUniqueId(), rawName),
                CommandMessages::homeNotFound,
                CommandMessages.HOME_WORLD_MISSING,
                CommandMessages::homeTeleported
        ));
    }

    private boolean handleSetWarp(CommandSender sender, String[] args) {
        return withPlayerAndExactArgs(sender, args, 1, (player, actualArgs) -> withInvalidNameGuard(player, () -> {
            TeleportPoint point = locationMapper.toDomain(player.getLocation());
            String warpName = warpService.setWarp(actualArgs[0], point);
            player.sendMessage(CommandMessages.warpSaved(warpName));
            return true;
        }));
    }

    private boolean handleDelHome(CommandSender sender, String[] args) {
        return withPlayerAndExactArgs(sender, args, 1, (player, actualArgs) -> withInvalidNameGuard(player, () -> {
            boolean deleted = homeService.deleteHome(player.getUniqueId(), actualArgs[0]);
            if (!deleted) {
                player.sendMessage(CommandMessages.homeNotFound(actualArgs[0]));
                return true;
            }
            player.sendMessage(CommandMessages.homeDeleted(normalized(actualArgs[0])));
            return true;
        }));
    }

    private boolean handleWarp(CommandSender sender, String[] args) {
        return withPlayerAndExactArgs(sender, args, 1, (player, actualArgs) -> withInvalidNameGuard(player, () -> {
            return executeTeleport(
                    player,
                    actualArgs[0],
                    warpService::findWarp,
                    CommandMessages::warpNotFound,
                    CommandMessages.WARP_WORLD_MISSING,
                    CommandMessages::warpTeleported
            );
        }));
    }

    private boolean handleWarps(CommandSender sender, String[] args) {
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

    private boolean handleDelWarp(CommandSender sender, String[] args) {
        return withPlayerAndExactArgs(sender, args, 1, (player, actualArgs) -> withInvalidNameGuard(player, () -> {
            boolean deleted = warpService.deleteWarp(actualArgs[0]);
            if (!deleted) {
                player.sendMessage(CommandMessages.warpNotFound(actualArgs[0]));
                return true;
            }
            player.sendMessage(CommandMessages.warpDeleted(normalized(actualArgs[0])));
            return true;
        }));
    }

    private boolean handleTpa(CommandSender sender, String[] args) {
        return withPlayerAndExactArgs(sender, args, 1, (requester, actualArgs) -> {
            Player target = requester.getServer().getPlayerExact(actualArgs[0]);
            if (target == null) {
                requester.sendMessage(CommandMessages.playerNotFound(actualArgs[0]));
                return true;
            }

            TpaService.RequestStatus status = tpaService.createRequest(
                    requester.getUniqueId(),
                    target.getUniqueId(),
                    System.currentTimeMillis()
            );
            if (status == TpaService.RequestStatus.SELF_REQUEST) {
                requester.sendMessage(CommandMessages.TPA_SELF);
                return true;
            }

            requester.sendMessage(CommandMessages.tpaSent(target.getName()));
            target.sendMessage(CommandMessages.tpaReceived(requester.getName()));
            return true;
        });
    }

    private boolean handleTpAccept(CommandSender sender, String[] args) {
        return withPlayerAndExactArgs(sender, args, 0, (target, actualArgs) -> {
            TpaService.DecisionResult result = tpaService.acceptRequest(target.getUniqueId(), System.currentTimeMillis());
            if (result.status() == TpaService.DecisionStatus.NO_PENDING) {
                target.sendMessage(CommandMessages.TPA_NO_PENDING);
                return true;
            }

            if (result.status() == TpaService.DecisionStatus.EXPIRED) {
                target.sendMessage(CommandMessages.TPA_EXPIRED);
                Player requester = target.getServer().getPlayer(result.requesterId());
                if (requester != null) {
                    requester.sendMessage(CommandMessages.TPA_REQUEST_EXPIRED_FOR_REQUESTER);
                }
                return true;
            }

            Player requester = target.getServer().getPlayer(result.requesterId());
            if (requester == null) {
                target.sendMessage(CommandMessages.playerNotFound("demandeur"));
                return true;
            }

            requester.teleport(target.getLocation());
            target.sendMessage(CommandMessages.tpaAcceptedTarget(requester.getName()));
            requester.sendMessage(CommandMessages.tpaAcceptedRequester(target.getName()));
            return true;
        });
    }

    private boolean handleTpDeny(CommandSender sender, String[] args) {
        return withPlayerAndExactArgs(sender, args, 0, (target, actualArgs) -> {
            TpaService.DecisionResult result = tpaService.denyRequest(target.getUniqueId(), System.currentTimeMillis());
            if (result.status() == TpaService.DecisionStatus.NO_PENDING) {
                target.sendMessage(CommandMessages.TPA_NO_PENDING);
                return true;
            }

            if (result.status() == TpaService.DecisionStatus.EXPIRED) {
                target.sendMessage(CommandMessages.TPA_EXPIRED);
                Player requester = target.getServer().getPlayer(result.requesterId());
                if (requester != null) {
                    requester.sendMessage(CommandMessages.TPA_REQUEST_EXPIRED_FOR_REQUESTER);
                }
                return true;
            }

            Player requester = target.getServer().getPlayer(result.requesterId());
            if (requester != null) {
                requester.sendMessage(CommandMessages.tpaDeniedRequester(target.getName()));
                target.sendMessage(CommandMessages.tpaDeniedTarget(requester.getName()));
            } else {
                target.sendMessage(CommandMessages.playerNotFound("demandeur"));
            }
            return true;
        });
    }

    private boolean executeTeleport(
            Player player,
            String rawName,
            TeleportPointLookup lookup,
            Function<String, String> notFoundMessage,
            String worldMissingMessage,
            Function<String, String> successMessage
    ) {
        Optional<TeleportPoint> maybePoint = lookup.find(rawName);
        if (maybePoint.isEmpty()) {
            player.sendMessage(notFoundMessage.apply(rawName));
            return true;
        }

        Optional<org.bukkit.Location> maybeLocation = locationMapper.toBukkit(maybePoint.get());
        if (maybeLocation.isEmpty()) {
            player.sendMessage(worldMissingMessage);
            return true;
        }

        String normalizedName = normalized(rawName);
        player.teleport(maybeLocation.get());
        player.sendMessage(successMessage.apply(normalizedName));
        return true;
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
            candidates = homeService.listHomes(player.getUniqueId());
        } else if ("warp".equals(commandName) || "delwarp".equals(commandName)) {
            candidates = warpService.listWarps();
        } else if ("tpa".equals(commandName) && sender instanceof Player player) {
            candidates = new ArrayList<>();
            for (Player online : player.getServer().getOnlinePlayers()) {
                if (!online.getUniqueId().equals(player.getUniqueId())) {
                    candidates.add(online.getName());
                }
            }
        } else {
            return Collections.emptyList();
        }

        return filterByPrefix(candidates, partial);
    }
}
