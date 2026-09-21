package com.yourserver.hideorhunt.listener;

import com.yourserver.hideorhunt.HideOrHuntPlugin;
import com.yourserver.hideorhunt.team.TeamData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerConnectionListener implements Listener {
    private final HideOrHuntPlugin plugin;

    public PlayerConnectionListener(HideOrHuntPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.updatePlayerTab(player);

        TeamData td = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (td != null) {
            if (!td.isBeaconAlive()) {
                player.sendMessage(Component.text("⚠ WARNING: Your team's beacon was shattered while you were gone! You are on your final life!", NamedTextColor.RED));
            }
        }
    }
}
