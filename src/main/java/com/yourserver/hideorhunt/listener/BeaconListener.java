package com.yourserver.hideorhunt.listener;

import com.yourserver.hideorhunt.HideOrHuntPlugin;
import com.yourserver.hideorhunt.team.BeaconTeam;
import com.yourserver.hideorhunt.team.TeamManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.time.Duration;

public class BeaconListener implements Listener {

    private final HideOrHuntPlugin plugin;
    private final TeamManager teamManager;

    public BeaconListener(HideOrHuntPlugin plugin, TeamManager teamManager) {
        this.plugin = plugin;
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

        // 1. Global sound to every online player
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.8f);
        }

        // Satisfying sound for the breaker
        breaker.playSound(breaker.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);

        // 2. Title screen alert for victims
        Title title = Title.title(
                Component.text("BEACON DESTROYED!", NamedTextColor.DARK_RED, TextDecoration.BOLD),
                Component.text("You are now on your FINAL LIFE. No respawns.", NamedTextColor.RED),
                Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(4), Duration.ofSeconds(1))
        );

        for (java.util.UUID uuid : victimTeam.getMembers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.showTitle(title);
            }
        }

        // 3. Global announcement
        Bukkit.broadcast(Component.text("» ", NamedTextColor.DARK_GRAY)
                .append(Component.text(victimTeam.getName() + "'s ", NamedTextColor.RED, TextDecoration.BOLD))
                .append(Component.text("Beacon was shattered by ", NamedTextColor.GRAY))
                .append(Component.text(breaker.getName(), NamedTextColor.YELLOW))
                .append(Component.text("! They are now in HARDCORE mode.", NamedTextColor.GRAY)));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;

        Block clicked = event.getClickedBlock();
        Player player = event.getPlayer();

        // Feature 2: Beacon acts as a Crafting Table
        if (clicked.getType() == Material.BEACON) {
            BeaconTeam team = teamManager.getTeam(player);
            // Allow members to craft from their beacon
            event.setCancelled(true); // Cancels the vanilla Beacon mineral upgrade GUI
            player.openWorkbench(clicked.getLocation(), true);
            return;
        }

        // Feature 2 & 3: Lock standard Crafting Tables unless enabled by Admin
        if (clicked.getType() == Material.CRAFTING_TABLE) {
            if (!plugin.isCraftingTableAllowed()) {
                event.setCancelled(true);
                player.sendMessage(Component.text("Crafting tables are disabled! You must craft directly from your Beacon.", NamedTextColor.RED));
            }
        }
    }
}
