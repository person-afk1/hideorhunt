package com.yourserver.hideorhunt.command;

import com.yourserver.hideorhunt.team.BeaconTeam;
import com.yourserver.hideorhunt.team.TeamManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class TeamCommand implements CommandExecutor {

    private final TeamManager teamManager;

    public TeamCommand(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length < 1) {
            sender.sendMessage(Component.text("Usage: /team <create|join|leave|givebeacon> [args]", NamedTextColor.RED));
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "create" -> {
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /team create <teamName>", NamedTextColor.RED));
                    return true;
                }
                String name = args[1];
                teamManager.createTeam(name);
                sender.sendMessage(Component.text("Created team: " + name, NamedTextColor.GREEN));
            }
            case "join" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Only players can join teams!");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(Component.text("Usage: /team join <teamName>", NamedTextColor.RED));
                    return true;
                }
                boolean joined = teamManager.joinTeam(player, args[1]);
                if (joined) {
                    player.sendMessage(Component.text("Joined team: " + args[1], NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text("Team not found!", NamedTextColor.RED));
                }
            }
            case "givebeacon" -> {
                if (!(sender instanceof Player player)) return true;
                player.getInventory().addItem(new ItemStack(Material.BEACON));
                player.sendMessage(Component.text("Received team Beacon!", NamedTextColor.AQUA));
            }
            default -> sender.sendMessage(Component.text("Unknown subcommand.", NamedTextColor.RED));
        }
        return true;
    }
}
