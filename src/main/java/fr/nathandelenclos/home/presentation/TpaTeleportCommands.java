package fr.nathandelenclos.home.presentation;

import fr.nathandelenclos.home.application.TpaService;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

final class TpaTeleportCommands {

    private final TpaService tpaService;

    TpaTeleportCommands(TpaService tpaService) {
        this.tpaService = tpaService;
    }

    boolean tpa(Player requester, String targetName) {
        Player target = requester.getServer().getPlayerExact(targetName);
        if (target == null) {
            requester.sendMessage(CommandMessages.playerNotFound(targetName));
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
    }

    boolean accept(Player target) {
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
    }

    boolean deny(Player target) {
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
    }

    List<String> completions(Player sender) {
        List<String> candidates = new ArrayList<>();
        for (Player online : sender.getServer().getOnlinePlayers()) {
            if (!online.getUniqueId().equals(sender.getUniqueId())) {
                candidates.add(online.getName());
            }
        }
        return candidates;
    }
}
