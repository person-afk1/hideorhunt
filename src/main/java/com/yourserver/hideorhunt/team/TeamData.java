package com.yourserver.hideorhunt.team;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class TeamData {
    private final String name;
    private final NamedTextColor color;
    private UUID leader;
    private final Set<UUID> members = new HashSet<>();
    private Location beaconLocation;
    private boolean beaconAlive = true;
    private boolean friendlyFire = false;

    public TeamData(String name, NamedTextColor color) {
        this.name = name;
        this.color = color;
    }

    public String getName() { return name; }
    public NamedTextColor getColor() { return color; }
    public UUID getLeader() { return leader; }
    public void setLeader(UUID leader) { this.leader = leader; }
    public Set<UUID> getMembers() { return members; }
    public Location getBeaconLocation() { return beaconLocation; }
    public void setBeaconLocation(Location beaconLocation) { this.beaconLocation = beaconLocation; }
    public boolean isBeaconAlive() { return beaconAlive; }
    public void setBeaconAlive(boolean beaconAlive) { this.beaconAlive = beaconAlive; }
    public boolean isFriendlyFire() { return friendlyFire; }
    public void setFriendlyFire(boolean friendlyFire) { this.friendlyFire = friendlyFire; }
}
