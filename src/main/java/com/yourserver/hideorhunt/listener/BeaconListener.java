package com.yourserver.hideorhunt.listener;

import com.yourserver.hideorhunt.HideOrHuntPlugin;
import com.yourserver.hideorhunt.team.TeamData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class BeaconListener implements Listener {
    private final HideOrHuntPlugin plugin;

    public BeaconListener(HideOrHuntPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBeaconPlace(BlockPlaceEvent event) {
        if (event.getBlock().getType() != Material.BEACON) return;
        Player player = event.getPlayer();
        TeamData td = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());

        if (td == null || !player.getUniqueId().equals(td.getLeader())) {
            event.setCancelled(true);
            player.sendMessage(Component.text("Only team leaders can place the beacon!", NamedTextColor.RED));
            return;
        }

        if (event.getBlock().getWorld().getEnvironment() != World.Environment.NORMAL) {
            event.setCancelled(true);
            player.sendMessage(Component.text("Beacons can only be placed in the Overworld!", NamedTextColor.RED));
            return;
        }

        if (event.getBlock().getY() < plugin.getMinBeaconY()) {
            event.setCancelled(true);
            player.sendMessage(Component.text("You cannot place your beacon below Y = " + plugin.getMinBeaconY() + "!", NamedTextColor.RED));
            return;
        }

        td.setBeaconLocation(event.getBlock().getLocation());
        plugin.getTeamManager().saveData();

        for (UUID uid : td.getMembers()) {
            Player mate = Bukkit.getPlayer(uid);
            if (mate != null) {
                mate.sendMessage(Component.text("» Your team's beacon has been established at X=" +
                        event.getBlock().getX() + ", Y=" + event.getBlock().getY() + ", Z=" + event.getBlock().getZ() + "!", NamedTextColor.GREEN));
            }
        }
    }

    @EventHandler
    public void onBeaconBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() != Material.BEACON) return;
        Player player = event.getPlayer();
        TeamData playerTeam = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());

        TeamData beaconTeam = null;
        for (TeamData td : plugin.getTeamManager().getTeams()) {
            if (td.getBeaconLocation() != null && td.getBeaconLocation().equals(event.getBlock().getLocation())) {
                beaconTeam = td;
                break;
            }
        }

        if (beaconTeam == null) return;

        HideOrHuntPlugin.GameState state = plugin.getGameState();
        if (state == HideOrHuntPlugin.GameState.GRACE_PERIOD) {
            if (playerTeam != null && playerTeam.equals(beaconTeam)) {
                beaconTeam.setBeaconLocation(null);
                player.sendMessage(Component.text("You retrieved your beacon during Grace Period.", NamedTextColor.YELLOW));
                return;
            } else {
                event.setCancelled(true);
                player.sendMessage(Component.text("You cannot destroy enemy beacons during Grace Period!", NamedTextColor.RED));
                return;
            }
        }

        if (playerTeam != null && playerTeam.equals(beaconTeam)) {
            event.setCancelled(true);
            player.sendMessage(Component.text("Your beacon is permanently locked and cannot be moved!", NamedTextColor.RED));
            return;
        }

        event.setDropItems(false);
        beaconTeam.setBeaconAlive(false);
        plugin.getTeamManager().saveData();

        Bukkit.broadcast(Component.text("» " + beaconTeam.getName() + "'s BEACON HAS BEEN DESTROYED BY " + player.getName() + "!", NamedTextColor.RED));
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 1f, 1f);
        }
    }

    @EventHandler
    public void onBeaconInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.BEACON) return;

        event.setCancelled(true);
        event.getPlayer().openWorkbench(null, true);
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (event.getRecipe() == null) return;
        if (!plugin.isCraftingAllowed() && event.getRecipe().getResult().getType() == Material.CRAFTING_TABLE) {
            event.getInventory().setResult(new ItemStack(Material.AIR));
        }
    }

    @EventHandler
    public void onExplosion(BlockExplodeEvent event) {
        event.blockList().removeIf(b -> b.getType() == Material.BEACON);
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(b -> b.getType() == Material.BEACON);
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block b : event.getBlocks()) {
            if (b.getType() == Material.BEACON) event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block b : event.getBlocks()) {
            if (b.getType() == Material.BEACON) event.setCancelled(true);
        }
    }
}
