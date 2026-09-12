package com.yourserver.hideorhunt.command;

import com.yourserver.hideorhunt.HideOrHuntPlugin;
import com.yourserver.hideorhunt.team.BeaconTeam;
import com.yourserver.hideorhunt.team.TeamManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AdminCommand implements CommandExecutor, TabCompleter {

    private final HideOrHuntPlugin plugin;
    private final TeamManager teamManager;

    public AdminCommand(HideOrHuntPlugin plugin, TeamManager teamManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("hideorhunt.admin")) {
            sender.sendMessage(Component.text("You don't have permission to use admin commands!", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "givebeacons" -> {
                int count = 0;
                for (Player online : Bukkit.getOnlinePlayers()) {
                    BeaconTeam team = teamManager.getTeam(online);
                    if (team != null && team.getBeaconLocation() == null) {
                        online.getInventory().addItem(new ItemStack(Material.BEACON, 1));
                        online.sendMessage(Component.text("You have received your team's Beacon!", NamedTextColor.GREEN));
                        count++;
                    }
                }
                sender.sendMessage(Component.text("Distributed beacons to " + count + " eligible player(s).", NamedTextColor.YELLOW));
            }

            case "forceteam" -> {
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Usage: /" + label + " forceteam <player> <teamName>", NamedTextColor.RED));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(Component.text("Player not found online!", NamedTextColor.RED));
                    return true;
                }
                String teamName = args[2];
                BeaconTeam team = teamManager.getTeamByName(teamName);
                if (team == null) {
                    team = new BeaconTeam(teamName);
                    teamManager.registerTeam(team);
                }
                team.addMember(target.getUniqueId());
                teamManager.setPlayerTeam(target.getUniqueId(), team);

                sender.sendMessage(Component.text("Force-assigned " + target.getName() + " to " + team.getName(), NamedTextColor.GREEN));
                target.sendMessage(Component.text("An admin placed you into team " + team.getName(), NamedTextColor.YELLOW));
            }

            case "destroybeacon" -> {
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /" + label + " destroybeacon <teamName>", NamedTextColor.RED));
                    return true;
                }
                BeaconTeam targetTeam = teamManager.getTeamByName(args[1]);
                if (targetTeam == null) {
                    sender.sendMessage(Component.text("Team not found!", NamedTextColor.RED));
                    return true;
                }
                if (!targetTeam.isBeaconAlive()) {
                    sender.sendMessage(Component.text("That team's beacon is already destroyed!", NamedTextColor.RED));
                    return true;
                }

                if (targetTeam.getBeaconLocation() != null) {
                    targetTeam.getBeaconLocation().getBlock().setType(Material.AIR);
                }
                targetTeam.destroyBeacon();

                Bukkit.broadcast(Component.text("⚡ [ADMIN EVENT] ", NamedTextColor.RED, TextDecoration.BOLD)
                        .append(Component.text(targetTeam.getName() + "'s Beacon was forcefully destroyed by an administrator!", NamedTextColor.GOLD)));
            }

            case "clearteams" -> {
                teamManager.getAllTeams().clear();
                sender.sendMessage(Component.text("All active teams have been cleared!", NamedTextColor.YELLOW));
            }

            case "status" -> {
                sender.sendMessage(Component.text("=== Hide or Hunt Admin Status ===", NamedTextColor.GOLD));
                for (BeaconTeam t : teamManager.getAllTeams()) {
                    NamedTextColor statusColor = t.isBeaconAlive() ? NamedTextColor.GREEN : NamedTextColor.RED;
                    String statusText = t.isBeaconAlive() ? (t.getBeaconLocation() == null ? "Not Placed" : "Placed (" + formatLoc(t) + ")") : "DESTROYED";
                    
                    sender.sendMessage(Component.text("• " + t.getName() + " (" + t.getMembers().size() + " members): ", NamedTextColor.WHITE)
                            .append(Component.text(statusText, statusColor)));
                }
            }

            default -> sendHelp(sender);
        }

        return true;
    }

    private String formatLoc(BeaconTeam team) {
        if (team.getBeaconLocation() == null) return "None";
        return team.getBeaconLocation().getBlockX() + ", " + team.getBeaconLocation().getBlockY() + ", " + team.getBeaconLocation().getBlockZ();
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("--- Hide or Hunt Admin Commands ---", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/hohadmin status", NamedTextColor.YELLOW).append(Component.text(" - View all teams and beacon states", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/hohadmin givebeacons", NamedTextColor.YELLOW).append(Component.text(" - Give 1 beacon to teams who haven't placed one", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/hohadmin forceteam <player> <team>", NamedTextColor.YELLOW).append(Component.text(" - Force a player into a team", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/hohadmin destroybeacon <team>", NamedTextColor.YELLOW).append(Component.text(" - Remotely destroy a team's beacon", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/hohadmin clearteams", NamedTextColor.YELLOW).append(Component.text(" - Reset and remove all teams", NamedTextColor.GRAY)));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("status", "givebeacons", "forceteam", "destroybeacon", "clearteams");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("destroybeacon")) {
            List<String> teamNames = new ArrayList<>();
            for (BeaconTeam t : teamManager.getAllTeams()) {
                teamNames.add(t.getName());
            }
            return teamNames;
        }
        return List.of();
    }
}
