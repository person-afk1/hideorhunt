package com.yourserver.hideorhunt.team;

import com.yourserver.hideorhunt.HideOrHuntPlugin;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class TeamManager {
    private final HideOrHuntPlugin plugin;
    private final Map<String, TeamData> teams = new HashMap<>();
    private final Map<UUID, String> playerTeamMap = new HashMap<>();
    private final Set<UUID> teamChatEnabled = new HashSet<>();
    private final File dataFile;
    private FileConfiguration dataConfig;

    public TeamManager(HideOrHuntPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
        initDefaultTeams();
        loadData();
    }

    private void initDefaultTeams() {
        registerTeam("Red", NamedTextColor.RED);
        registerTeam("Blue", NamedTextColor.BLUE);
        registerTeam("Green", NamedTextColor.GREEN);
        registerTeam("Yellow", NamedTextColor.YELLOW);
    }

    public void registerTeam(String name, NamedTextColor color) {
        teams.put(name.toLowerCase(), new TeamData(name, color));
    }

    public TeamData getTeam(String name) {
        return teams.get(name.toLowerCase());
    }

    public TeamData getPlayerTeam(UUID uuid) {
        String teamName = playerTeamMap.get(uuid);
        return teamName != null ? teams.get(teamName) : null;
    }

    public void setPlayerTeam(UUID uuid, String teamName) {
        TeamData oldTeam = getPlayerTeam(uuid);
        if (oldTeam != null) {
            oldTeam.getMembers().remove(uuid);
        }
        TeamData team = getTeam(teamName);
        if (team != null) {
            team.getMembers().add(uuid);
            playerTeamMap.put(uuid, teamName.toLowerCase());
        } else {
            playerTeamMap.remove(uuid);
        }
        saveData();
    }

    public Collection<TeamData> getTeams() {
        return teams.values();
    }

    public boolean isTeamChatActive(UUID uuid) {
        return teamChatEnabled.contains(uuid);
    }

    public void toggleTeamChat(UUID uuid) {
        if (!teamChatEnabled.add(uuid)) {
            teamChatEnabled.remove(uuid);
        }
    }

    public void saveData() {
        if (dataConfig == null) dataConfig = new YamlConfiguration();
        for (TeamData team : teams.values()) {
            String path = "teams." + team.getName().toLowerCase();
            dataConfig.set(path + ".leader", team.getLeader() != null ? team.getLeader().toString() : null);
            List<String> mems = team.getMembers().stream().map(UUID::toString).toList();
            dataConfig.set(path + ".members", mems);
            dataConfig.set(path + ".beaconAlive", team.isBeaconAlive());
            dataConfig.set(path + ".friendlyFire", team.isFriendlyFire());
            if (team.getBeaconLocation() != null) {
                dataConfig.set(path + ".beaconLoc", team.getBeaconLocation());
            }
        }
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save data.yml: " + e.getMessage());
        }
    }

    public void loadData() {
        if (!dataFile.exists()) return;
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        if (!dataConfig.contains("teams")) return;

        for (String key : dataConfig.getConfigurationSection("teams").getKeys(false)) {
            TeamData team = getTeam(key);
            if (team == null) continue;
            String path = "teams." + key;
            if (dataConfig.contains(path + ".leader")) {
                String leadStr = dataConfig.getString(path + ".leader");
                if (leadStr != null) team.setLeader(UUID.fromString(leadStr));
            }
            team.setBeaconAlive(dataConfig.getBoolean(path + ".beaconAlive", true));
            team.setFriendlyFire(dataConfig.getBoolean(path + ".friendlyFire", false));
            if (dataConfig.contains(path + ".beaconLoc")) {
                team.setBeaconLocation(dataConfig.getLocation(path + ".beaconLoc"));
            }
            List<String> mems = dataConfig.getStringList(path + ".members");
            for (String m : mems) {
                UUID uid = UUID.fromString(m);
                team.getMembers().add(uid);
                playerTeamMap.put(uid, key.toLowerCase());
            }
        }
    }
}
