package com.yourserver.hideorhunt.team;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BeaconTeam {
    private final String name;
    private final Set<UUID> members = new HashSet<>();
    private Location beaconLocation = null;
    private boolean beaconAlive = true;

    public BeaconTeam(String name) {
        this.name = name;
    }

    public void addMember(UUID uuid) {
        members.add(uuid);
    }

    public boolean isBeaconAlive() {
        return beaconAlive;
    }

    public void destroyBeacon() {
        this.beaconAlive = false;
        this.beaconLocation = null;
    }

    public Location getBeaconLocation() {
        return beaconLocation;
    }

    public void setBeaconLocation(Location loc) {
        this.beaconLocation = loc;
    }

    public Set<UUID> getMembers() {
        return members;
    }

    public String getName() {
        return name;
    }

    public void broadcast(Component message) {
        for (UUID uuid : members) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.sendMessage(message);
            }
        }
    }
}
