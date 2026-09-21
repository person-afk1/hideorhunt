package com.yourserver.hideorhunt.listener;

import com.yourserver.hideorhunt.HideOrHuntPlugin;
import com.yourserver.hideorhunt.team.TeamData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class CombatListener implements Listener {
    private final HideOrHuntPlugin plugin;

    public CombatListener(HideOrHuntPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        Player attacker = null;
        if (event.getDamager() instanceof Player p) {
            attacker = p;
        } else if (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            attacker = p;
        }

        if (attacker == null) return;

        if (plugin.getGameState() == HideOrHuntPlugin.GameState.GRACE_PERIOD) {
            event.setCancelled(true);
            attacker.sendMessage(Component.text("Grace Period is currently active!", NamedTextColor.RED));
            return;
        }

        TeamData vTeam = plugin.getTeamManager().getPlayerTeam(victim.getUniqueId());
        TeamData aTeam = plugin.getTeamManager().getPlayerTeam(attacker.getUniqueId());

        if (vTeam != null && aTeam != null && vTeam.equals(aTeam)) {
            if (!vTeam.isFriendlyFire()) {
                event.setCancelled(true);
                attacker.sendMessage(Component.text("Friendly fire is disabled for your team!", NamedTextColor.RED));
            }
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        TeamData td = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());

        if (td != null && !td.isBeaconAlive()) {
            player.setGameMode(GameMode.SPECTATOR);
            plugin.updatePlayerTab(player);

            for (Player online : Bukkit.getOnlinePlayers()) {
                online.playSound(online.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
            }

            Bukkit.broadcast(Component.text("» " + player.getName() + " has been permanently eliminated! (" + td.getName() + ")", NamedTextColor.DARK_RED));
            plugin.checkWinCondition();
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        TeamData td = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());

        if (td != null && td.isBeaconAlive() && td.getBeaconLocation() != null) {
            Location respawnLoc = td.getBeaconLocation().clone().add(0.5, 1.0, 0.5);
            event.setRespawnLocation(respawnLoc);

            new BukkitRunnable() {
                @Override
                public void run() {
                    plugin.giveKit(player);

                    if (plugin.getGameState() == HideOrHuntPlugin.GameState.GLOWING_ACTIVE) {
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (player.isOnline() && player.getGameMode() == GameMode.SURVIVAL) {
                                    player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 6000, 0));
                                }
                            }
                        }.runTaskLater(plugin, 35 * 20L);
                    }
                }
            }.runTaskLater(plugin, 1L);
        }
    }
}
