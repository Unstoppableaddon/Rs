package com.example.battleroyale;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class BattleRoyalePlugin extends JavaPlugin {
    private static BattleRoyalePlugin instance;
    private GameManager gameManager;

    public static BattleRoyalePlugin get() { return instance; }
    public GameManager game() { return gameManager; }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        migrateDefaults();

        gameManager = new GameManager(this);
        if (getCommand("br") != null) {
            getCommand("br").setExecutor(new Commands(gameManager));
        }
        getServer().getPluginManager().registerEvents(new Listeners(gameManager), this);

        getLogger().info("BattleRoyale enabled.");
    }

    @Override
    public void onDisable() {
        if (gameManager != null) gameManager.forceReset();
        getLogger().info("BattleRoyale disabled.");
    }

    private void migrateDefaults() {
        FileConfiguration c = getConfig();
        c.addDefault("arena.world", Bukkit.getWorlds().get(0).getName());
        c.addDefault("arena.center.x", 0.0);
        c.addDefault("arena.center.y", 100.0);
        c.addDefault("arena.center.z", 0.0);
        c.addDefault("arena.spawn.radius", 120.0);
        c.addDefault("border.start", 250.0);
        c.addDefault("border.end", 20.0);
        c.addDefault("border.shrinkSeconds", 420);
        c.addDefault("lobby.countdownSeconds", 15);
        c.addDefault("kit.giveOnStart", true);
        c.options().copyDefaults(true);
        saveConfig();
    }
}
