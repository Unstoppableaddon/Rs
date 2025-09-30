package com.example.battleroyale;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class GameManager {
    public enum State { IDLE, LOBBY, LIVE, ENDING }

    private final BattleRoyalePlugin plugin;
    private final Set<UUID> queue = new HashSet<>();
    private final Set<UUID> alive = new HashSet<>();
    private Location arenaCenter;
    private double spawnRadius;
    private double borderStart, borderEnd; int borderShrinkSeconds; int countdownSeconds;
    private World world;
    private State state = State.IDLE;

    public GameManager(BattleRoyalePlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        FileConfiguration c = plugin.getConfig();
        world = Bukkit.getWorld(c.getString("arena.world"));
        if (world == null) world = Bukkit.getWorlds().get(0);
        arenaCenter = new Location(world,
                c.getDouble("arena.center.x"),
                c.getDouble("arena.center.y"),
                c.getDouble("arena.center.z"));
        spawnRadius = c.getDouble("arena.spawn.radius");
        borderStart = c.getDouble("border.start");
        borderEnd = c.getDouble("border.end");
        borderShrinkSeconds = c.getInt("border.shrinkSeconds");
        countdownSeconds = c.getInt("lobby.countdownSeconds");
    }

    public State state() { return state; }

    public boolean join(Player p) {
        if (state != State.IDLE && state != State.LOBBY) { p.sendMessage(prefix("A match is already live.")); return false; }
        queue.add(p.getUniqueId());
        p.sendMessage(prefix("Joined the queue. Players: " + queue.size()));
        state = State.LOBBY;
        return true;
    }

    public boolean leave(Player p) {
        boolean removed = queue.remove(p.getUniqueId()) | alive.remove(p.getUniqueId());
        if (removed) p.sendMessage(prefix("Left Battle Royale."));
        if (queue.isEmpty() && state == State.LOBBY) state = State.IDLE;
        return removed;
    }

    public void setCenter(Player p) {
        Location l = p.getLocation();
        plugin.getConfig().set("arena.world", l.getWorld().getName());
        plugin.getConfig().set("arena.center.x", l.getX());
        plugin.getConfig().set("arena.center.y", l.getY());
        plugin.getConfig().set("arena.center.z", l.getZ());
        plugin.saveConfig();
        loadConfig();
        p.sendMessage(prefix("Arena center set to your current location."));
    }

    public void start(Player invoker) {
        if (state != State.LOBBY || queue.size() < 2) { invoker.sendMessage(prefix("Need at least 2 players in lobby.")); return; }
        state = State.LIVE;
        alive.clear();
        alive.addAll(queue);

        // Prepare world
        world.setGameRule(GameRule.KEEP_INVENTORY, false);
        world.setPVP(true);
        world.setDifficulty(Difficulty.HARD);
        world.getWorldBorder().setCenter(arenaCenter);
        world.getWorldBorder().setSize(borderStart);

        // Spread players around a ring
        List<Player> players = alive.stream().map(Bukkit::getPlayer).filter(Objects::nonNull).collect(Collectors.toList());
        for (int i = 0; i < players.size(); i++) {
            double angle = (2 * Math.PI * i) / players.size();
            double x = arenaCenter.getX() + spawnRadius * Math.cos(angle + randJitter());
            double z = arenaCenter.getZ() + spawnRadius * Math.sin(angle + randJitter());
            Location spawn = new Location(world, x, arenaCenter.getY(), z);
            spawn = world.getHighestBlockAt(spawn).getLocation().add(0.5, 1, 0.5);
            Player p = players.get(i);
            prepPlayer(p);
            p.teleport(spawn);
            p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20*6, 255, false, false, false));
        }

        // Countdown & shrink
        new BukkitRunnable(){
            int t = countdownSeconds;
            public void run(){
                if (t <= 0) {
                    broadcast("Match started! Border is shrinking.");
                    world.getWorldBorder().setSize(borderEnd, borderShrinkSeconds);
                    cancel();
                } else {
                    broadcast("Match begins in " + t + "s");
                    t -= 5; if (t < 0) t = 0;
                }
            }
        }.runTaskTimer(plugin, 0L, 100L); // every 5s
    }

    private void prepPlayer(Player p) {
        p.getInventory().clear();
        p.setHealth(Objects.requireNonNull(p.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getDefaultValue());
        p.setFoodLevel(20);
        p.setSaturation(20f);
        giveKit(p);
        p.setGameMode(GameMode.SURVIVAL);
    }

    private void giveKit(Player p) {
        if (!plugin.getConfig().getBoolean("kit.giveOnStart", true)) return;
        p.getInventory().addItem(new ItemStack(Material.STONE_SWORD));
        p.getInventory().addItem(new ItemStack(Material.BOW));
        p.getInventory().addItem(new ItemStack(Material.ARROW, 16));
        p.getInventory().addItem(new ItemStack(Material.BREAD, 8));
        ItemStack chest = new ItemStack(Material.LEATHER_CHESTPLATE);
        LeatherArmorMeta meta = (LeatherArmorMeta) chest.getItemMeta();
        if (meta != null) { meta.setColor(Color.fromRGB(60, 120, 200)); chest.setItemMeta(meta); }
        p.getInventory().setChestplate(chest);
    }

    public void handleDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        alive.remove(p.getUniqueId());
        Bukkit.getScheduler().runTask(plugin, () -> p.spigot().respawn());
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            p.setGameMode(GameMode.SPECTATOR);
            p.teleport(arenaCenter);
            p.sendMessage(prefix("You are out. Good luck to the remaining players!"));
        }, 2L);
        checkWin();
    }

    public void handleQuit(Player p) {
        if (state == State.LIVE && alive.remove(p.getUniqueId())) checkWin();
        queue.remove(p.getUniqueId());
    }

    private void checkWin() {
        if (state != State.LIVE) return;
        if (alive.size() <= 1) {
            state = State.ENDING;
            Player winner = alive.stream().map(Bukkit::getPlayer).filter(Objects::nonNull).findFirst().orElse(null);
            broadcast(winner != null ? (winner.getName() + " wins the Battle Royale!") : "No winner.");
            new BukkitRunnable(){ public void run(){ forceReset(); } }.runTaskLater(plugin, 60L);
        }
    }

    public void forceReset() {
        // Reset world border and players
        world.getWorldBorder().setSize(borderStart);
        for (UUID id : queue) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) { p.setGameMode(GameMode.SURVIVAL); p.getInventory().clear(); }
        }
        queue.clear();
        alive.clear();
        state = State.IDLE;
        broadcast("Battle Royale reset. Use /br join to queue.");
    }

    private void broadcast(String msg) { Bukkit.broadcastMessage(prefix(msg)); }
    private String prefix(String s) { return ChatColor.GOLD + "[BR] " + ChatColor.WHITE + s; }
    private double randJitter(){ return ThreadLocalRandom.current().nextDouble(-Math.PI/24, Math.PI/24); }
}
