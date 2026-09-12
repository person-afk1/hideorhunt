package com.yourserver.hideorhunt.team;

import com.yourserver.hideorhunt.HideOrHuntPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

public class TeamManager {
    private final HideOrHuntPlugin plugin;
    private final Map<String, BeaconTeam> teamsByName = new HashMap<>();
    private final Map<UUID, BeaconTeam> playerTeams = new HashMap<>();

    public TeamManager(HideOrHuntPlugin plugin) {
        this.plugin = plugin;
    }

    public HideOrHuntPlugin getPlugin() {
        return plugin;
    }

    public void registerTeam(BeaconTeam team) {
        teamsByName.put(team.getName().toLowerCase(), team);
    }

    public BeaconTeam getTeamByName(String name) {
        return teamsByName.get(name.toLowerCase());
    }

    public BeaconTeam getTeam(Player player) {
        return playerTeams.get(player.getUniqueId());
    }

    public void setPlayerTeam(UUID uuid, BeaconTeam team) {
        playerTeams.put(uuid, team);
    }

    public Collection<BeaconTeam> getAllTeams() {
        return teamsByName.values();
    }

    public BeaconTeam getTeamByBeaconLocation(Location loc) {
        if (loc == null) return null;
        for (BeaconTeam team : teamsByName.values()) {
            Location beaconLoc = team.getBeaconLocation();
            if (beaconLoc != null && beaconLoc.getWorld().equals(loc.getWorld())
                    && beaconLoc.getBlockX() == loc.getBlockX()
                    && beaconLoc.getBlockY() == loc.getBlockY()
                    && beaconLoc.getBlockZ() == loc.getBlockZ()) {
                return team;
            }
        }
        return null;
    }
}
