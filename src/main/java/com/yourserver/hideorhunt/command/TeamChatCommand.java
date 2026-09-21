package com.yourserver.hideorhunt.command;

import com.yourserver.hideorhunt.HideOrHuntPlugin;
import com.yourserver.hideorhunt.team.TeamData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class TeamChatCommand implements CommandExecutor {
    private final HideOrHuntPlugin plugin;

    public TeamChatCommand(HideOrHuntPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        TeamData td = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (td == null) {
            player.sendMessage(Component.text("You are not currently in a team!", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            plugin.getTeamManager().toggleTeamChat(player.getUniqueId());
            boolean active = plugin.getTeamManager().isTeamChatActive(player.getUniqueId());
            player.sendMessage(Component.text("Team chat mode is now: " + (active ? "ENABLED" : "DISABLED"), active ? NamedTextColor.GREEN : NamedTextColor.RED));
            return true;
        }

        String msg = String.join(" ", args);
        Component tcMsg = Component.text("[TeamChat] ", td.getColor())
                .append(Component.text(player.getName() + ": ", NamedTextColor.WHITE))
                .append(Component.text(msg));

        for (UUID uid : td.getMembers()) {
            Player mate = Bukkit.getPlayer(uid);
            if (mate != null) mate.sendMessage(tcMsg);
        }
        return true;
    }
}
