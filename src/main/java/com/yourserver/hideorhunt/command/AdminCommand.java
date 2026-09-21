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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AdminCommand implements CommandExecutor, TabCompleter {
    private final HideOrHuntPlugin plugin;

    public AdminCommand(HideOrHuntPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("carnage.admin")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "startgame" -> {
                int radius = args.length > 1 ? Integer.parseInt(args[1]) : 500;
                Player p = sender instanceof Player ? (Player) sender : null;
                plugin.startGame(p != null ? p.getWorld() : Bukkit.getWorlds().get(0), radius);
                sender.sendMessage(Component.text("Match started with border radius " + radius, NamedTextColor.GREEN));
            }
            case "pause" -> {
                plugin.pauseGame();
                sender.sendMessage(Component.text("Match paused.", NamedTextColor.YELLOW));
            }
            case "resume" -> {
                plugin.resumeGame();
                sender.sendMessage(Component.text("Match resumed.", NamedTextColor.GREEN));
            }
            case "startglowing" -> {
                plugin.startGlowingCycle();
                sender.sendMessage(Component.text("Glowing phase triggered.", NamedTextColor.GREEN));
            }
            case "stopglowing" -> {
                plugin.stopGlowingCycle();
                sender.sendMessage(Component.text("Glowing stopped.", NamedTextColor.YELLOW));
            }
            case "setleader" -> {
                if (args.length < 3) return false;
                Player target = Bukkit.getPlayer(args[1]);
                TeamData td = plugin.getTeamManager().getTeam(args[2]);
                if (target != null && td != null) {
                    td.setLeader(target.getUniqueId());
                    plugin.getTeamManager().setPlayerTeam(target.getUniqueId(), td.getName());
                    sender.sendMessage(Component.text(target.getName() + " is now leader of " + td.getName(), NamedTextColor.GREEN));
                }
            }
            case "setplayer" -> {
                if (args.length < 3) return false;
                Player target = Bukkit.getPlayer(args[1]);
                TeamData td = plugin.getTeamManager().getTeam(args[2]);
                if (target != null && td != null) {
                    plugin.getTeamManager().setPlayerTeam(target.getUniqueId(), td.getName());
                    plugin.updatePlayerTab(target);
                    sender.sendMessage(Component.text("Assigned " + target.getName() + " to " + td.getName(), NamedTextColor.GREEN));
                }
            }
            case "setminy" -> {
                if (args.length < 2) return false;
                int y = Integer.parseInt(args[1]);
                plugin.setMinBeaconY(y);
                sender.sendMessage(Component.text("Minimum beacon placement level set to Y = " + y, NamedTextColor.GREEN));
            }
            case "togglecrafting" -> {
                boolean next = !plugin.isCraftingAllowed();
                plugin.setCraftingAllowed(next);
                sender.sendMessage(Component.text("Vanilla crafting tables allowed: " + next, NamedTextColor.GREEN));
            }
            case "pvp" -> {
                if (args.length < 3) return false;
                TeamData td = plugin.getTeamManager().getTeam(args[1]);
                boolean on = args[2].equalsIgnoreCase("on");
                if (td != null) {
                    td.setFriendlyFire(on);
                    plugin.getTeamManager().saveData();
                    sender.sendMessage(Component.text("Friendly fire for " + td.getName() + " set to " + on, NamedTextColor.GREEN));
                }
            }
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== Tournament Host Controls ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/hohadmin startgame [radius] - Drops players and initializes border", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/hohadmin pause / resume - Freezes match events, damage, and timers", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/hohadmin setleader <player> <team> - Assigns team leader with beacon", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/hohadmin setplayer <player> <team> - Assigns member to a team", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/hohadmin setminy <y> - Set lowest allowed beacon Y level", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/hohadmin startglowing / stopglowing - Manually cycle glow status", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/hohadmin togglecrafting - Toggle vanilla crafting table crafting", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/hohadmin pvp <team> <on|off> - Change specific team friendly fire", NamedTextColor.YELLOW));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("carnage.admin")) return List.of();
        if (args.length == 1) {
            return filter(Arrays.asList("startgame", "pause", "resume", "setleader", "setplayer", "setminy", "startglowing", "stopglowing", "togglecrafting", "pvp", "help"), args[0]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("setleader") || args[0].equalsIgnoreCase("setplayer"))) {
            return null;
        }
        if (args.length == 3 && (args[0].equalsIgnoreCase("setleader") || args[0].equalsIgnoreCase("setplayer") || args[0].equalsIgnoreCase("pvp"))) {
            return filter(plugin.getTeamManager().getTeams().stream().map(TeamData::getName).toList(), args[2]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("pvp")) {
            return filter(List.of("on", "off"), args[2]);
        }
        return List.of();
    }

    private List<String> filter(List<String> list, String prefix) {
        List<String> out = new ArrayList<>();
        for (String s : list) {
            if (s.toLowerCase().startsWith(prefix.toLowerCase())) out.add(s);
        }
        return out;
    }
}
