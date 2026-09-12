package com.yourserver.hideorhunt.team;

import com.yourserver.hideorhunt.HideOrHuntPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeamManager {

    private final HideOrHuntPlugin plugin;
    
    // Stores teams by their lowercase name (e.g. "red" -> BeaconTeam)
    private final Map<String, BeaconTeam> teams = new HashMap<>();
    
    // Stores which player is on which team (Player UUID -> BeaconTeam)
    private final Map<UUID, BeaconTeam> playerTeamMap = new HashMap<>();

    public TeamManager(HideOrHuntPlugin plugin) {
        this.plugin = plugin;
    }

    public HideOrHuntPlugin getPlugin() {
        return plugin;
    }

    // 1. Create a new team
    public BeaconTeam createTeam(String name) {
        BeaconTeam team = new BeaconTeam(name);
        teams.put(name.toLowerCase(), team);
        return team;
    }

    // 2. Add a player to a team
    public boolean joinTeam(Player player, String teamName) {
        BeaconTeam targetTeam = teams.get(teamName.toLowerCase());
        if (targetTeam == null) {
            return false;
        }

        // Leave current team first if they are in one
        leaveTeam(player);

        targetTeam.addMember(player.getUniqueId());
        playerTeamMap.put(player.getUniqueId(), targetTeam);
        return true;
    }

    // 3. Remove a player from their team
    public void leaveTeam(Player player) {
        BeaconTeam currentTeam = playerTeamMap.remove(player.getUniqueId());
        if (currentTeam != null) {
            currentTeam.removeMember(player.getUniqueId());
        }
    }

    // 4. Find which team a player belongs to
    public BeaconTeam getTeam(Player player) {
        return playerTeamMap.get(player.getUniqueId());
    }

    // 5. Find a team by their beacon's block location
    public BeaconTeam getTeamByBeaconLocation(Location location) {
        for (BeaconTeam team : teams.values()) {
            Location bLoc = team.getBeaconLocation();
            if (bLoc != null && bLoc.getWorld().equals(location.getWorld())
                    && bLoc.getBlockX() == location.getBlockX()
                    && bLoc.getBlockY() == location.getBlockY()
                    && bLoc.getBlockZ() == location.getBlockZ()) {
                return team;
            }
        }
        return null;
    }

    public Collection<BeaconTeam> getTeams() {
        return teams.values();
    }
}
