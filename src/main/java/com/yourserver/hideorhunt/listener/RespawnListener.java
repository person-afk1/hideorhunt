package com.yourserver.hideorhunt.listener;

import com.yourserver.hideorhunt.team.BeaconTeam;
import com.yourserver.hideorhunt.team.TeamManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class RespawnListener implements Listener {

    private final TeamManager teamManager;
    private final Set<UUID> eliminatedPlayers = new HashSet<>();

    public RespawnListener(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getPlayer();
        BeaconTeam team = teamManager.getTeam(victim);

        if (team == null) return;

        if (!team.isBeaconAlive()) {
            eliminatedPlayers.add(victim.getUniqueId());
            Bukkit.broadcast(Component.text("☠ ", NamedTextColor.DARK_RED)
                    .append(Component.text(victim.getName(), NamedTextColor.WHITE))
                    .append(Component.text(" was permanently eliminated!", NamedTextColor.RED)));
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        if (eliminatedPlayers.contains(player.getUniqueId())) {
            Bukkit.getScheduler().runTask(teamManager.getPlugin(), () -> {
                player.setGameMode(GameMode.SPECTATOR);
                player.sendMessage(Component.text("You are eliminated and now spectating.", NamedTextColor.GRAY));
            });
            return;
        }

        BeaconTeam team = teamManager.getTeam(player);
        if (team != null && team.isBeaconAlive() && team.getBeaconLocation() != null) {
            Location respawnLoc = team.getBeaconLocation().clone().add(0.5, 1.0, 0.5);
            event.setRespawnLocation(respawnLoc);
            player.sendMessage(Component.text("Respawned at your team's Beacon.", NamedTextColor.GREEN));
        }
    }
}
