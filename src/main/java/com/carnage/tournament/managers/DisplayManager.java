package com.carnage.tournament.managers;

import com.carnage.tournament.CarnagePlugin;
import com.carnage.tournament.models.GameState;
import com.carnage.tournament.models.TeamData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.UUID;

public class DisplayManager {
    private final CarnagePlugin plugin;
    private final Scoreboard mainScoreboard;

    public DisplayManager(CarnagePlugin plugin) {
        this.plugin = plugin;
        this.mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        setupTeams();
    }

    private void setupTeams() {
        for (TeamData td : plugin.getTeamManager().getTeams()) {
            Team sbTeam = mainScoreboard.getTeam(td.getName().toLowerCase());
            if (sbTeam == null) {
                sbTeam = mainScoreboard.registerNewTeam(td.getName().toLowerCase());
            }
            sbTeam.color(td.getColor());
            sbTeam.prefix(Component.text("[" + td.getName() + "] ", td.getColor()));
        }
        Team specTeam = mainScoreboard.getTeam("z_spectator");
        if (specTeam == null) {
            specTeam = mainScoreboard.registerNewTeam("z_spectator");
        }
        specTeam.color(NamedTextColor.GRAY);
        specTeam.prefix(Component.text("[Dead] ", NamedTextColor.DARK_GRAY));
    }

    public void updatePlayerTab(Player player) {
        player.setScoreboard(mainScoreboard);
        player.sendPlayerListHeaderAndFooter(
                Component.text("✦ Carnage SMP - Hide or Hunt ✦", NamedTextColor.GOLD, TextDecoration.BOLD),
                Component.text("Online: ", NamedTextColor.GRAY).append(Component.text(Bukkit.getOnlinePlayers().size(), NamedTextColor.YELLOW))
        );

        if (player.getGameMode() == GameMode.SPECTATOR) {
            Team spec = mainScoreboard.getTeam("z_spectator");
            if (spec != null) spec.addPlayer(player);
            return;
        }

        TeamData td = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (td != null) {
            Team sbTeam = mainScoreboard.getTeam(td.getName().toLowerCase());
            if (sbTeam != null) sbTeam.addPlayer(player);
        }
    }

    public void updateAllScoreboards(int timerSeconds) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            renderScoreboard(p, timerSeconds);
        }
    }

    private void renderScoreboard(Player player, int seconds) {
        Scoreboard board = player.getScoreboard();
        Objective obj = board.getObjective("carnage_board");
        if (obj == null) {
            obj = board.registerNewObjective("carnage_board", Criteria.DUMMY,
                    Component.text("HIDE OR HUNT", NamedTextColor.GOLD, TextDecoration.BOLD));
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        for (String entry : board.getEntries()) {
            board.resetScores(entry);
        }

        int line = 15;
        obj.getScore("§7§m-------------------").setScore(line--);

        GameState state = plugin.getGameManager().getGameState();
        String timerFmt = String.format("%02d:%02d", seconds / 60, seconds % 60);

        if (state == GameState.GRACE_PERIOD) {
            obj.getScore("§eGrace Ends: §f" + timerFmt).setScore(line--);
        } else if (state == GameState.GLOWING_ACTIVE) {
            obj.getScore("§c§lGLOWING: §f" + timerFmt).setScore(line--);
        } else if (state == GameState.GLOWING_COOLDOWN || state == GameState.ACTIVE_PVP) {
            obj.getScore("§aGlow Cycle: §f" + timerFmt).setScore(line--);
        } else if (state == GameState.PAUSED) {
            obj.getScore("§c§lPAUSED").setScore(line--);
        } else {
            obj.getScore("§7Waiting for Host").setScore(line--);
        }

        obj.getScore("§1").setScore(line--);

        for (TeamData td : plugin.getTeamManager().getTeams()) {
            int alive = 0;
            for (UUID u : td.getMembers()) {
                Player tp = Bukkit.getPlayer(u);
                if (tp != null && tp.getGameMode() != GameMode.SPECTATOR) alive++;
            }
            String icon = td.isBeaconAlive() ? "§a✔" : "§c❌";
            obj.getScore(icon + " " + td.getColor() + td.getName() + "§7: §f" + alive).setScore(line--);
        }

        obj.getScore("§7§m------------------- ").setScore(line--);
    }
}
