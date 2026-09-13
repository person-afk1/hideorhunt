package com.yourserver.hideorhunt.command;

import com.yourserver.hideorhunt.HideOrHuntPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class AdminCommand implements CommandExecutor {

    private final HideOrHuntPlugin plugin;

    public AdminCommand(HideOrHuntPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("hideorhunt.admin")) {
            sender.sendMessage(Component.text("You do not have permission to run this command.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(Component.text("Usage: /hohadmin togglecrafting", NamedTextColor.RED));
            return true;
        }

        if (args[0].equalsIgnoreCase("togglecrafting")) {
            boolean newState = !plugin.isCraftingTableAllowed();
            plugin.setCraftingTableAllowed(newState);

            NamedTextColor color = newState ? NamedTextColor.GREEN : NamedTextColor.RED;
            String status = newState ? "ENABLED" : "DISABLED";

            sender.sendMessage(Component.text("Standard Crafting Tables are now " + status + ".", color));
            return true;
        }

        sender.sendMessage(Component.text("Unknown subcommand.", NamedTextColor.RED));
        return true;
    }
}
