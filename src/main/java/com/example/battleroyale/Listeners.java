package com.example.battleroyale;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class Listeners implements Listener {
    private final GameManager gm;
    public Listeners(GameManager gm){ this.gm = gm; }

    @EventHandler
    public void onDeath(PlayerDeathEvent e){ gm.handleDeath(e); }

    @EventHandler
    public void onQuit(PlayerQuitEvent e){ gm.handleQuit(e.getPlayer()); }
}
