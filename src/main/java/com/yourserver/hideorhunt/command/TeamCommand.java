package com.yourserver.hideorhunt.command;

import com.yourserver.hideorhunt.team.BeaconTeam;
import com.yourserver.hideorhunt.team.TeamManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TeamCommand implements CommandExecutor {

    private final TeamManager teamManager;

    public TeamCommand(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(Component.text("Usage: /team <create|join|list>", NamedTextColor.RED));
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("create")) {
            if (args.length < 2) {
                player.sendMessage(Component.text("Usage: /team create <teamName>", NamedTextColor.RED));
                return true;
            }
            String teamName = args[1];
            if (teamManager.getTeamByName(teamName) != null) {
                player.sendMessage(Component.text("A team with that name already exists!", NamedTextColor.RED));
                return true;
            }

            BeaconTeam team = new BeaconTeam(teamName);
            teamManager.registerTeam(team);
            team.addMember(player.getUniqueId());
            teamManager.setPlayerTeam(player.getUniqueId(), team);

            player.sendMessage(Component.text("Team '" + teamName + "' created and you have joined it!", NamedTextColor.GREEN));
            return true;
        }

        if (sub.equals("join")) {
            if (args.length < 2) {
                player.sendMessage(Component.text("Usage: /team join <teamName>", NamedTextColor.RED));
                return true;
            }
            String teamName = args[1];
            BeaconTeam team = teamManager.getTeamByName(teamName);

            if (team == null) {
                player.sendMessage(Component.text("Team not found!", NamedTextColor.RED));
                return true;
            }

            team.addMember(player.getUniqueId());
            teamManager.setPlayerTeam(player.getUniqueId(), team);
            player.sendMessage(Component.text("You joined team " + teamName + "!", NamedTextColor.GREEN));
            return true;
        }

        if (sub.equals("list")) {
            player.sendMessage(Component.text("--- Active Teams ---", NamedTextColor.GOLD));
            for (BeaconTeam t : teamManager.getAllTeams()) {
                player.sendMessage(Component.text("- " + t.getName() + " (" + t.getMembers().size() + " members)", NamedTextColor.YELLOW));
            }
            return true;
        }

        return false;
    }
}
