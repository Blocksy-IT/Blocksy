package it.blocksy;

import it.blocksy.api.BlocksyAPI;
import it.blocksy.api.BlocksyReward;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

/**
 * Sistema di polling per recuperare i premi Ruota della Fortuna dall'API
 */
public class RewardChecker {
    
    private final Blocksy plugin;
    private final BlocksyAPI api;
    private final String apiKey;
    private final int checkInterval;
    private BukkitTask task;
    private boolean running;
    
    public RewardChecker(Blocksy plugin, String apiKey, int checkInterval) {
        this.plugin = plugin;
        this.api = new BlocksyAPI();
        this.apiKey = apiKey;
        this.checkInterval = checkInterval;
        this.running = false;
    }
    
    public void start() {
        if (running) return;
        running = true;
        
        long intervalTicks = checkInterval * 20L;
        
        task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            checkForRewards();
        }, 60L, intervalTicks);
    }
    
    public void stop() {
        if (!running) return;
        running = false;
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
    
    private void checkForRewards() {
        try {
            // Raccogli lista player online per filtrare i premi dall'API
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!first) sb.append(",");
                sb.append(p.getName());
                first = false;
            }
            String onlineList = sb.toString();
            
            if (onlineList.isEmpty()) return; // Inutile controllare se nessuno è online
            
            List<BlocksyReward> rewards = api.fetchRewards(apiKey, onlineList);
            if (rewards.isEmpty()) return;
            
            plugin.getLogger().info("Trovati " + rewards.size() + " premi Ruota della Fortuna da riscattare.");
            
            for (BlocksyReward reward : rewards) {
                processReward(reward);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Errore nel controllo premi: " + e.getMessage());
        }
    }
    
    private void processReward(BlocksyReward reward) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayer(reward.getUsername());
            if (player != null && player.isOnline()) {
                // Supporta sia %player% (vecchio) che {player} (nuovo docs)
                String cmd = reward.getCommand()
                        .replace("%player%", player.getName())
                        .replace("{player}", player.getName());
                
                plugin.getLogger().info("§a[Blocksy] Eseguendo premio per " + player.getName() + ": " + cmd);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            } else {
                plugin.getLogger().warning("§c[Blocksy] Impossibile consegnare premio a " + reward.getUsername() + " (player non più online).");
            }
        });
    }
}
