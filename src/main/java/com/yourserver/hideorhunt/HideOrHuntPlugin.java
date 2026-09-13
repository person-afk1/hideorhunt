package com.yourserver.hideorhunt;

import com.yourserver.hideorhunt.command.AdminCommand;
import com.yourserver.hideorhunt.command.TeamCommand;
import com.yourserver.hideorhunt.listener.BeaconListener;
import com.yourserver.hideorhunt.listener.RespawnListener;
import com.yourserver.hideorhunt.team.TeamManager;
import org.bukkit.plugin.java.JavaPlugin;

public class HideOrHuntPlugin extends JavaPlugin {

    private TeamManager teamManager;
    private boolean craftingTableAllowed = false; // Default: blocked

    @Override
    public void onEnable() {
        this.teamManager = new TeamManager(this);

        // Register Listeners (now passes 'this' along with 'teamManager')
        getServer().getPluginManager().registerEvents(new BeaconListener(this, teamManager), this);
        getServer().getPluginManager().registerEvents(new RespawnListener(teamManager), this);

        // Register Commands
        if (this.getCommand("team") != null) {
            this.getCommand("team").setExecutor(new TeamCommand(teamManager));
        }
        if (this.getCommand("hohadmin") != null) {
            this.getCommand("hohadmin").setExecutor(new AdminCommand(this));
        }

        getLogger().info("HideOrHunt has been enabled successfully!");
    }

    public boolean isCraftingTableAllowed() {
        return craftingTableAllowed;
    }

    public void setCraftingTableAllowed(boolean allowed) {
        this.craftingTableAllowed = allowed;
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }
}
