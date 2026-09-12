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
    private final Map<String, BeaconTeam> teams = new HashMap<>();
    private final Map<UUID, BeaconTeam> playerTeamMap = new HashMap<>();

    public TeamManager(HideOrHuntPlugin plugin) {
        this.plugin = plugin;
    }

    public HideOrHuntPlugin getPlugin() {
        return plugin;
    }

    // --- Methods for TeamCommand ---

    public BeaconTeam createTeam(String name) {
        BeaconTeam team = new BeaconTeam(name);
        teams.put(name.toLowerCase(), team);
        return team;
    }

    public boolean joinTeam(Player player, String teamName) {
        BeaconTeam targetTeam = teams.get(teamName.toLowerCase());
        if (targetTeam == null) {
            return false;
        }
        leaveTeam(player);
        targetTeam.addMember(player.getUniqueId());
        playerTeamMap.put(player.getUniqueId(), targetTeam);
        return true;
    }

    public void leaveTeam(Player player) {
        BeaconTeam currentTeam = playerTeamMap.remove(player.getUniqueId());
        if (currentTeam != null) {
            currentTeam.removeMember(player.getUniqueId());
        }
    }

    public BeaconTeam getTeam(Player player) {
        return playerTeamMap.get(player.getUniqueId());
    }

    public Collection<BeaconTeam> getTeams() {
        return teams.values();
    }

    // --- Methods required by AdminCommand ---

    public BeaconTeam getTeamByName(String name) {
        if (name == null) return null;
        return teams.get(name.toLowerCase());
    }

    public void registerTeam(BeaconTeam team) {
        if (team != null) {
            teams.put(team.getName().toLowerCase(), team);
        }
    }

    public void setPlayerTeam(UUID uuid, BeaconTeam team) {
        if (team == null) {
            BeaconTeam prev = playerTeamMap.remove(uuid);
            if (prev != null) prev.removeMember(uuid);
            return;
        }
        BeaconTeam prev = playerTeamMap.get(uuid);
        if (prev != null) prev.removeMember(uuid);

        team.addMember(uuid);
        playerTeamMap.put(uuid, team);
    }

    public Collection<BeaconTeam> getAllTeams() {
        return teams.values();
    }

    // --- Beacon lookup ---

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
}
