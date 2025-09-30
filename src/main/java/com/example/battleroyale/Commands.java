package com.example.battleroyale;

import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class Commands implements CommandExecutor {
    private final GameManager gm;
    public Commands(GameManager gm){ this.gm = gm; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) { help(sender); return true; }
        switch (args[0].toLowerCase()){
            case "join":
                if (!(sender instanceof Player)) { sender.sendMessage("Players only."); return true; }
                gm.join((Player) sender); return true;
            case "leave":
                if (!(sender instanceof Player)) { sender.sendMessage("Players only."); return true; }
                gm.leave((Player) sender); return true;
            case "start":
                if (!sender.hasPermission("br.admin")) { sender.sendMessage(ChatColor.RED+"No permission."); return true; }
                if (sender instanceof Player) gm.start((Player) sender); else sender.sendMessage("Console cannot start; use a player.");
                return true;
            case "setcenter":
                if (!(sender instanceof Player)) { sender.sendMessage("Players only."); return true; }
                if (!sender.hasPermission("br.admin")) { sender.sendMessage(ChatColor.RED+"No permission."); return true; }
                gm.setCenter((Player) sender); return true;
            case "status":
                sender.sendMessage(ChatColor.GOLD+"[BR] State: "+gm.state());
                return true;
            default: help(sender); return true;
        }
    }

    private void help(CommandSender s){
        s.sendMessage(ChatColor.GOLD+"BattleRoyale Commands:");
        s.sendMessage(ChatColor.YELLOW+"/br join"+ChatColor.WHITE+" – queue for the next match");
        s.sendMessage(ChatColor.YELLOW+"/br leave"+ChatColor.WHITE+" – leave the queue or match");
        s.sendMessage(ChatColor.YELLOW+"/br status"+ChatColor.WHITE+" – show state");
        s.sendMessage(ChatColor.YELLOW+"/br start"+ChatColor.WHITE+" – (admin) start the match");
        s.sendMessage(ChatColor.YELLOW+"/br setcenter"+ChatColor.WHITE+" – (admin) set arena center at your location");
    }
}
