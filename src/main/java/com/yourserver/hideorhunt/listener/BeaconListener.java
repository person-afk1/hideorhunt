package com.yourserver.hideorhunt.listener;

import com.yourserver.hideorhunt.team.BeaconTeam;
import com.yourserver.hideorhunt.team.TeamManager;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.time.Duration;

public class BeaconListener implements Listener {

    private final TeamManager teamManager;

    public BeaconListener(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @EventHandler
    public void onBeaconPlace(BlockPlaceEvent event) {
        if (event.getBlock().getType() != Material.BEACON) return;

        Player player = event.getPlayer();
        BeaconTeam team = teamManager.getTeam(player);

        if (team == null) {
            player.sendMessage(Component.text("You are not on a team!", NamedTextColor.RED));
            event.setCancelled(true);
            return;
        }

        if (team.getBeaconLocation() != null) {
            player.sendMessage(Component.text("Your team already placed a beacon!", NamedTextColor.RED));
            event.setCancelled(true);
            return;
        }

        team.setBeaconLocation(event.getBlock().getLocation());
        team.broadcast(Component.text("Your Beacon has been placed! Defend it at all costs.", NamedTextColor.GREEN));
    }

    @EventHandler
    public void onBeaconBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.BEACON) return;

        BeaconTeam victimTeam = teamManager.getTeamByBeaconLocation(block.getLocation());
        if (victimTeam == null) return;

        Player breaker = event.getPlayer();
        BeaconTeam breakerTeam = teamManager.getTeam(breaker);

        if (breakerTeam != null && breakerTeam.equals(victimTeam)) {
            breaker.sendMessage(Component.text("You cannot break your own Beacon!", NamedTextColor.RED));
            event.setCancelled(true);
            return;
        }

        victimTeam.destroyBeacon();

        Title title = Title.title(
                Component.text("BEACON DESTROYED!", NamedTextColor.DARK_RED, TextDecoration.BOLD),
                Component.text("You are now on your FINAL LIFE. No respawns.", NamedTextColor.RED),
                Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(4), Duration.ofSeconds(1))
        );

        for (java.util.UUID uuid : victimTeam.getMembers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.showTitle(title);
                p.playSound(Sound.sound(org.bukkit.Sound.ENTITY_WITHER_SPAWN.key(), Sound.Source.MASTER, 1f, 0.8f));
            }
        }

        Bukkit.broadcast(Component.text("» ", NamedTextColor.DARK_GRAY)
                .append(Component.text(victimTeam.getName() + "'s ", NamedTextColor.RED, TextDecoration.BOLD))
                .append(Component.text("Beacon was shattered by ", NamedTextColor.GRAY))
                .append(Component.text(breaker.getName(), NamedTextColor.YELLOW))
                .append(Component.text("! They are now in HARDCORE mode.", NamedTextColor.GRAY)));
    }
}
