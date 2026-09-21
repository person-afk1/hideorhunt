package com.carnage.tournament;

import com.carnage.tournament.commands.AdminCommand;
import com.carnage.tournament.commands.TeamChatCommand;
import com.carnage.tournament.commands.TeamCommand;
import com.carnage.tournament.listeners.BeaconListener;
import com.carnage.tournament.listeners.CombatListener;
import com.carnage.tournament.listeners.MatchControlListener;
import com.carnage.tournament.listeners.PlayerConnectionListener;
import com.carnage.tournament.managers.DisplayManager;
import com.carnage.tournament.managers.GameManager;
import com.carnage.tournament.managers.TeamManager;
import org.bukkit.plugin.java.JavaPlugin;

public class CarnagePlugin extends JavaPlugin {
    private TeamManager teamManager;
    private GameManager gameManager;
    private DisplayManager displayManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.teamManager = new TeamManager(this);
        this.gameManager = new GameManager(this);
        this.displayManager = new DisplayManager(this);

        registerCommands();
        registerEvents();

        getLogger().info("Carnage Hide or Hunt plugin enabled successfully!");
    }

    @Override
    public void onDisable() {
        if (teamManager != null) {
            teamManager.saveData();
        }
    }

    private void registerCommands() {
        AdminCommand adminCmd = new AdminCommand(this);
        getCommand("hohadmin").setExecutor(adminCmd);
        getCommand("hohadmin").setTabCompleter(adminCmd);

        TeamCommand teamCmd = new TeamCommand(this);
        getCommand("team").setExecutor(teamCmd);
        getCommand("team").setTabCompleter(teamCmd);

        getCommand("teamchat").setExecutor(new TeamChatCommand(this));
    }

    private void registerEvents() {
        getServer().getPluginManager().registerEvents(new BeaconListener(this), this);
        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        getServer().getPluginManager().registerEvents(new MatchControlListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
    }

    public TeamManager getTeamManager() { return teamManager; }
    public GameManager getGameManager() { return gameManager; }
    public DisplayManager getDisplayManager() { return displayManager; }
}
