package com.yourserver.hideorhunt.command;

import com.yourserver.hideorhunt.HideOrHuntPlugin;
import com.yourserver.hideorhunt.team.TeamData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class TeamCommand implements CommandExecutor, TabCompleter {
    private final HideOrHuntPlugin plugin;

    public TeamCommand(HideOrHuntPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("pvp")) {
            if (args.length < 2) {
                sender.sendMessage(Component.text("Usage: /team pvp <on|off>", NamedTextColor.RED));
                return true;
            }
            boolean enabled = args[1].equalsIgnoreCase("on");

            if (sender.hasPermission("carnage.admin")) {
                for (TeamData td : plugin.getTeamManager().getTeams()) {
                    td.setFriendlyFire(enabled);
                }
                plugin.getTeamManager().saveData();
                Bukkit.broadcast(Component.text("» Admin set Friendly Fire to " + (enabled ? "ON" : "OFF") + " globally!", NamedTextColor.YELLOW));
                return true;
            }

            if (!(sender instanceof Player p)) return true;
            TeamData td = plugin.getTeamManager().getPlayerTeam(p.getUniqueId());
            if (td == null || !p.getUniqueId().equals(td.getLeader())) {
                p.sendMessage(Component.text("Only team leaders or admins can toggle friendly fire!", NamedTextColor.RED));
                return true;
            }

            td.setFriendlyFire(enabled);
            plugin.getTeamManager().saveData();
            for (UUID uid : td.getMembers()) {
                Player mate = Bukkit.getPlayer(uid);
                if (mate != null) {
                    mate.sendMessage(Component.text("» Friendly fire set to " + (enabled ? "ON" : "OFF") + " by leader.", NamedTextColor.YELLOW));
                }
            }
            return true;
        }

        if (!(sender instanceof Player player)) return true;
        TeamData td = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (td == null) {
            player.sendMessage(Component.text("You are not on a team.", NamedTextColor.RED));
            return true;
        }

        player.sendMessage(Component.text("=== Your Team: " + td.getName() + " ===", td.getColor()));
        Player leader = td.getLeader() != null ? Bukkit.getPlayer(td.getLeader()) : null;
        player.sendMessage(Component.text("Leader: " + (leader != null ? leader.getName() : "None"), NamedTextColor.WHITE));
        player.sendMessage(Component.text("Beacon: " + (td.isBeaconAlive() ? "Alive" : "Destroyed"), td.isBeaconAlive() ? NamedTextColor.GREEN : NamedTextColor.RED));
        if (td.getBeaconLocation() != null) {
            player.sendMessage(Component.text("Beacon Coords: X=" + td.getBeaconLocation().getBlockX() + ", Y=" + td.getBeaconLocation().getBlockY() + ", Z=" + td.getBeaconLocation().getBlockZ(), NamedTextColor.GRAY));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) return List.of("pvp");
        if (args.length == 2 && args[0].equalsIgnoreCase("pvp")) return List.of("on", "off");
        return List.of();
    }
}
