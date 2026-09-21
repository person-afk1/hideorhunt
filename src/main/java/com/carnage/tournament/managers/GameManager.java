package com.carnage.tournament.managers;

import com.carnage.tournament.CarnagePlugin;
import com.carnage.tournament.models.GameState;
import com.carnage.tournament.models.TeamData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.Random;
import java.util.UUID;

public class GameManager {
    private final CarnagePlugin plugin;
    private GameState gameState = GameState.WAITING;
    private GameState savedPrePauseState = GameState.WAITING;
    private int currentTimer = 0;
    private boolean craftingAllowed = false;
    private int minBeaconY = 0;

    public GameManager(CarnagePlugin plugin) {
        this.plugin = plugin;
        this.minBeaconY = plugin.getConfig().getInt("min-beacon-y", 0);
        this.craftingAllowed = plugin.getConfig().getBoolean("allow-vanilla-crafting", false);
        startGameLoop();
    }

    public GameState getGameState() { return gameState; }
    public void setGameState(GameState state) { this.gameState = state; }
    public int getMinBeaconY() { return minBeaconY; }
    public void setMinBeaconY(int y) { this.minBeaconY = y; }
    public boolean isCraftingAllowed() { return craftingAllowed; }
    public void setCraftingAllowed(boolean allowed) { this.craftingAllowed = allowed; }

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

            TeamData td = plugin.getTeamManager().getPlayerTeam(p.getUniqueId());
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
        currentTimer = plugin.getConfig().getInt("grace-period-seconds", 600);
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
                    plugin.getDisplayManager().updateAllScoreboards(currentTimer);
                    return;
                }

                currentTimer--;

                if (gameState == GameState.GRACE_PERIOD && currentTimer <= 0) {
                    gameState = GameState.ACTIVE_PVP;
                    currentTimer = plugin.getConfig().getInt("glowing-start-seconds", 1200);
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

                plugin.getDisplayManager().updateAllScoreboards(currentTimer);
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public void startGlowingCycle() {
        gameState = GameState.GLOWING_ACTIVE;
        currentTimer = plugin.getConfig().getInt("glowing-duration-seconds", 600);
        Bukkit.broadcast(Component.text("» GLOWING PHASE ACTIVE! All players are revealed!", NamedTextColor.GOLD));
        applyGlowingToLiving();
    }

    public void stopGlowingCycle() {
        gameState = GameState.GLOWING_COOLDOWN;
        currentTimer = plugin.getConfig().getInt("glowing-cooldown-seconds", 300);
        Bukkit.broadcast(Component.text("» Glowing cycle ended. Cooldown active.", NamedTextColor.YELLOW));
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.removePotionEffect(PotionEffectType.GLOWING);
        }
    }

    public void applyGlowingToLiving() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getGameMode() == GameMode.SURVIVAL) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, currentTimer * 20, 0, false, false));
            }
        }
    }

    public void checkWinCondition() {
        TeamData survivingTeam = null;
        int activeTeams = 0;

        for (TeamData td : plugin.getTeamManager().getTeams()) {
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
            triggerEndgame(survivingTeam);
        }
    }

    private void triggerEndgame(TeamData winner) {
        Title victoryTitle = Title.title(
                Component.text("VICTORY!", NamedTextColor.GOLD),
                Component.text("Your team survived Hide or Hunt!", NamedTextColor.YELLOW),
                Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(5), Duration.ofSeconds(1))
        );

        Title defeatTitle = Title.title(
                Component.text("DEFEAT", NamedTextColor.DARK_RED),
                Component.text("Team " + winner.getName() + " has conquered the hunt.", NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(5), Duration.ofSeconds(1))
        );

        for (Player p : Bukkit.getOnlinePlayers()) {
            TeamData td = plugin.getTeamManager().getPlayerTeam(p.getUniqueId());
            if (td != null && td.getName().equalsIgnoreCase(winner.getName())) {
                p.showTitle(victoryTitle);
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                spawnFireworks(p.getLocation());
            } else {
                p.showTitle(defeatTitle);
                p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 1f, 0.7f);
            }
        }
    }

    private void spawnFireworks(Location loc) {
        Firework fw = loc.getWorld().spawn(loc, Firework.class);
        FireworkMeta fwm = fw.getFireworkMeta();
        fwm.addEffect(FireworkEffect.builder().withColor(Color.YELLOW, Color.ORANGE).with(FireworkEffect.Type.BALL_LARGE).build());
        fwm.setPower(1);
        fw.setFireworkMeta(fwm);
    }
}
