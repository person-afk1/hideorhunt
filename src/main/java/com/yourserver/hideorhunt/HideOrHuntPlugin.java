package com.yourserver.hideorhunt;

import com.yourserver.hideorhunt.command.AdminCommand;
import com.yourserver.hideorhunt.command.TeamChatCommand;
import com.yourserver.hideorhunt.command.TeamCommand;
import com.yourserver.hideorhunt.listener.BeaconListener;
import com.yourserver.hideorhunt.listener.CombatListener;
import com.yourserver.hideorhunt.listener.MatchControlListener;
import com.yourserver.hideorhunt.listener.PlayerConnectionListener;
import com.yourserver.hideorhunt.team.TeamData;
import com.yourserver.hideorhunt.team.TeamManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

import java.time.Duration;
import java.util.Random;
import java.util.UUID;

public class HideOrHuntPlugin extends JavaPlugin {
    public enum GameState {
        WAITING, GRACE_PERIOD, ACTIVE_PVP, GLOWING_ACTIVE, GLOWING_COOLDOWN, PAUSED, ENDED
    }

    private TeamManager teamManager;
    private GameState gameState = GameState.WAITING;
    private GameState savedPrePauseState = GameState.WAITING;
    private int currentTimer = 0;
    private boolean craftingAllowed = false;
    private int minBeaconY = 0;
    private Scoreboard mainScoreboard;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.minBeaconY = getConfig().getInt("min-beacon-y", 0);
        this.craftingAllowed = getConfig().getBoolean("allow-vanilla-crafting", false);
        this.mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

        this.teamManager = new TeamManager(this);
        setupScoreboardTeams();

        registerCommands();
        registerEvents();
        startGameLoop();

        getLogger().info("Carnage Hide or Hunt plugin enabled successfully!");
    }

    @Override
    public void onDisable() {
        if (teamManager != null) {
            teamManager.saveData();
        }
    }

    private void setupScoreboardTeams() {
        for (TeamData td : teamManager.getTeams()) {
            Team sbTeam = mainScoreboard.getTeam(td.getName().toLowerCase());
            if (sbTeam == null) sbTeam = mainScoreboard.registerNewTeam(td.getName().toLowerCase());
            sbTeam.color(td.getColor());
            sbTeam.prefix(Component.text("[" + td.getName() + "] ", td.getColor()));
        }
        Team specTeam = mainScoreboard.getTeam("z_spectator");
        if (specTeam == null) specTeam = mainScoreboard.registerNewTeam("z_spectator");
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

        TeamData td = teamManager.getPlayerTeam(player.getUniqueId());
        if (td != null) {
            Team sbTeam = mainScoreboard.getTeam(td.getName().toLowerCase());
            if (sbTeam != null) sbTeam.addPlayer(player);
        }
    }

    public void updateAllScoreboards(int timerSeconds) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            Scoreboard board = p.getScoreboard();
            Objective obj = board.getObjective("carnage_board");
            if (obj == null) {
                obj = board.registerNewObjective("carnage_board", Criteria.DUMMY,
                        Component.text("HIDE OR HUNT", NamedTextColor.GOLD, TextDecoration.BOLD));
                obj.setDisplaySlot(DisplaySlot.SIDEBAR);
            }

            for (String entry : board.getEntries()) board.resetScores(entry);

            int line = 15;
            obj.getScore("§7§m-------------------").setScore(line--);

            String timerFmt = String.format("%02d:%02d", timerSeconds / 60, timerSeconds % 60);
            if (gameState == GameState.GRACE_PERIOD) {
                obj.getScore("§eGrace Ends: §f" + timerFmt).setScore(line--);
            } else if (gameState == GameState.GLOWING_ACTIVE) {
                obj.getScore("§c§lGLOWING: §f" + timerFmt).setScore(line--);
            } else if (gameState == GameState.GLOWING_COOLDOWN || gameState == GameState.ACTIVE_PVP) {
                obj.getScore("§aGlow Cycle: §f" + timerFmt).setScore(line--);
            } else if (gameState == GameState.PAUSED) {
                obj.getScore("§c§lPAUSED").setScore(line--);
            } else {
                obj.getScore("§7Waiting for Host").setScore(line--);
            }

            obj.getScore("§1").setScore(line--);

            for (TeamData td : teamManager.getTeams()) {
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

    public void pauseGame() {
        if (gameState == GameState.PAUSED) return;
        savedPrePauseState = gameState;
        gameState = GameState.PAUSED;
        Bukkit.broadcast(Component.text("» MATCH HAS BEEN PAUSED BY AN ADMIN «", NamedTextColor.RED));
    }

    public void resumeGame() {
        if (gameState != GameState.PAUSED) return;
        gameState = savedPrePauseState;
        Bukkit.broadcast(Component.text("» MATCH RESUMED «", NamedTextColor.GREEN));
    }

    public void startGame(World world, int borderSize) {
        WorldBorder border = world.getWorldBorder();
        border.setCenter(0.0, 0.0);
        border.setSize(borderSize);

        Random rand = new Random();
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.getInventory().clear();
            p.setGameMode(GameMode.SURVIVAL);
            p.setHealth(20.0);
            p.setFoodLevel(20);

            giveKit(p);

            TeamData td = teamManager.getPlayerTeam(p.getUniqueId());
            if (td != null && p.getUniqueId().equals(td.getLeader())) {
                p.getInventory().addItem(new ItemStack(Material.BEACON, 1));
            }

            int x = rand.nextInt(borderSize - 40) - (borderSize / 2 - 20);
            int z = rand.nextInt(borderSize - 40) - (borderSize / 2 - 20);
            Location dropLoc = new Location(world, x, 250, z);
            p.teleport(dropLoc);
            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 600, 1, false, false));
        }

        gameState = GameState.GRACE_PERIOD;
        currentTimer = getConfig().getInt("grace-period-seconds", 600);
        Bukkit.broadcast(Component.text("» Hide or Hunt Started! 10-Minute Grace Period Active.", NamedTextColor.GREEN));
    }

    public void giveKit(Player player) {
        player.getInventory().addItem(new ItemStack(Material.IRON_INGOT, 40));
        player.getInventory().addItem(new ItemStack(Material.OAK_PLANKS, 64));

        ItemStack pot = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) pot.getItemMeta();
        meta.addCustomEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 3600, 0), true);
        meta.displayName(Component.text("3-Minute Invisibility", NamedTextColor.AQUA));
        pot.setItemMeta(meta);
        player.getInventory().addItem(pot);
    }

    private void startGameLoop() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (gameState == GameState.WAITING || gameState == GameState.PAUSED || gameState == GameState.ENDED) {
                    updateAllScoreboards(currentTimer);
                    return;
                }

                currentTimer--;

                if (gameState == GameState.GRACE_PERIOD && currentTimer <= 0) {
                    gameState = GameState.ACTIVE_PVP;
                    currentTimer = getConfig().getInt("glowing-start-seconds", 1200);
                    Bukkit.broadcast(Component.text("» Grace Period ended! Beacons are permanently locked. PvP Enabled!", NamedTextColor.RED));
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1f);
                    }
                } else if (gameState == GameState.ACTIVE_PVP && currentTimer <= 0) {
                    startGlowingCycle();
                } else if (gameState == GameState.GLOWING_ACTIVE && currentTimer <= 0) {
                    stopGlowingCycle();
                } else if (gameState == GameState.GLOWING_COOLDOWN && currentTimer <= 0) {
                    startGlowingCycle();
                }

                updateAllScoreboards(currentTimer);
            }
        }.runTaskTimer(this, 20L, 20L);
    }

    public void startGlowingCycle() {
        gameState = GameState.GLOWING_ACTIVE;
        currentTimer = getConfig().getInt("glowing-duration-seconds", 600);
        Bukkit.broadcast(Component.text("» GLOWING PHASE ACTIVE! All players are revealed!", NamedTextColor.GOLD));
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getGameMode() == GameMode.SURVIVAL) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, currentTimer * 20, 0, false, false));
            }
        }
    }

    public void stopGlowingCycle() {
        gameState = GameState.GLOWING_COOLDOWN;
        currentTimer = getConfig().getInt("glowing-cooldown-seconds", 300);
        Bukkit.broadcast(Component.text("» Glowing cycle ended. Cooldown active.", NamedTextColor.YELLOW));
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.removePotionEffect(PotionEffectType.GLOWING);
        }
    }

    public void checkWinCondition() {
        TeamData survivingTeam = null;
        int activeTeams = 0;

        for (TeamData td : teamManager.getTeams()) {
            boolean hasAlive = false;
            for (UUID uid : td.getMembers()) {
                Player p = Bukkit.getPlayer(uid);
                if (p != null && p.getGameMode() == GameMode.SURVIVAL) {
                    hasAlive = true;
                    break;
                }
            }
            if (hasAlive) {
                activeTeams++;
                survivingTeam = td;
            }
        }

        if (activeTeams == 1 && survivingTeam != null) {
            gameState = GameState.ENDED;
            Title victoryTitle = Title.title(
                    Component.text("VICTORY!", NamedTextColor.GOLD),
                    Component.text("Your team survived Hide or Hunt!", NamedTextColor.YELLOW),
                    Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(5), Duration.ofSeconds(1))
            );

            Title defeatTitle = Title.title(
                    Component.text("DEFEAT", NamedTextColor.DARK_RED),
                    Component.text("Team " + survivingTeam.getName() + " has conquered the hunt.", NamedTextColor.GRAY),
                    Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(5), Duration.ofSeconds(1))
            );

            for (Player p : Bukkit.getOnlinePlayers()) {
                TeamData td = teamManager.getPlayerTeam(p.getUniqueId());
                if (td != null && td.getName().equalsIgnoreCase(survivingTeam.getName())) {
                    p.showTitle(victoryTitle);
                    p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                    Firework fw = p.getWorld().spawn(p.getLocation(), Firework.class);
                    FireworkMeta fwm = fw.getFireworkMeta();
                    fwm.addEffect(FireworkEffect.builder().withColor(Color.YELLOW, Color.ORANGE).with(FireworkEffect.Type.BALL_LARGE).build());
                    fwm.setPower(1);
                    fw.setFireworkMeta(fwm);
                } else {
                    p.showTitle(defeatTitle);
                    p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 1f, 0.7f);
                }
            }
        }
    }

    public TeamManager getTeamManager() { return teamManager; }
    public GameState getGameState() { return gameState; }
    public int getMinBeaconY() { return minBeaconY; }
    public void setMinBeaconY(int y) { this.minBeaconY = y; }
    public boolean isCraftingAllowed() { return craftingAllowed; }
    public void setCraftingAllowed(boolean allowed) { this.craftingAllowed = allowed; }
}
