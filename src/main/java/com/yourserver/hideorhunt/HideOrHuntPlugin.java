package com.yourserver.hideorhunt;

import com.yourserver.hideorhunt.command.AdminCommand;
import com.yourserver.hideorhunt.command.TeamCommand;
import com.yourserver.hideorhunt.listener.BeaconListener;
import com.yourserver.hideorhunt.listener.RespawnListener;
import com.yourserver.hideorhunt.team.TeamManager;
import org.bukkit.plugin.java.JavaPlugin;

public class HideOrHuntPlugin extends JavaPlugin {

    private TeamManager teamManager;

    @Override
    public void onEnable() {
        this.teamManager = new TeamManager(this);

        getServer().getPluginManager().registerEvents(new BeaconListener(teamManager), this);
        getServer().getPluginManager().registerEvents(new RespawnListener(teamManager), this);

        if (getCommand("team") != null) {
            getCommand("team").setExecutor(new TeamCommand(teamManager));
        }

        if (getCommand("hohadmin") != null) {
            AdminCommand adminCmd = new AdminCommand(this, teamManager);
            getCommand("hohadmin").setExecutor(adminCmd);
            getCommand("hohadmin").setTabCompleter(adminCmd);
        }

        getLogger().info("HideOrHunt has been enabled successfully!");
    }

    @Override
    public void onDisable() {
        getLogger().info("HideOrHunt disabled.");
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }
}
