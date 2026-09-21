package com.yourserver.hideorhunt.listener;

import com.yourserver.hideorhunt.HideOrHuntPlugin;
import com.yourserver.hideorhunt.team.TeamData;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.UUID;

public class MatchControlListener implements Listener {
    private final HideOrHuntPlugin plugin;

    public MatchControlListener(HideOrHuntPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (plugin.getGameState() == HideOrHuntPlugin.GameState.PAUSED) {
            if (event.getFrom().getX() != event.getTo().getX() ||
                event.getFrom().getY() != event.getTo().getY() ||
                event.getFrom().getZ() != event.getTo().getZ()) {
                event.setTo(event.getFrom());
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (plugin.getGameState() == HideOrHuntPlugin.GameState.PAUSED) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (plugin.getGameState() == HideOrHuntPlugin.GameState.PAUSED) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (plugin.getGameState() == HideOrHuntPlugin.GameState.PAUSED) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        if (player.getGameMode() == GameMode.SPECTATOR) {
            event.setCancelled(true);
            Component msg = Component.text("[Spectator] " + player.getName() + ": ", NamedTextColor.GRAY).append(event.message());
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getGameMode() == GameMode.SPECTATOR || p.hasPermission("carnage.admin")) {
                    p.sendMessage(msg);
                }
            }
            return;
        }

        if (plugin.getTeamManager().isTeamChatActive(player.getUniqueId())) {
            event.setCancelled(true);
            TeamData td = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
            if (td == null) {
                player.sendMessage(Component.text("You are not in a team!", NamedTextColor.RED));
                return;
            }
            Component tcMsg = Component.text("[TeamChat] ", td.getColor())
                    .append(Component.text(player.getName() + ": ", NamedTextColor.WHITE))
                    .append(event.message());

            for (UUID uid : td.getMembers()) {
                Player mate = Bukkit.getPlayer(uid);
                if (mate != null) mate.sendMessage(tcMsg);
            }
        }
    }
}
