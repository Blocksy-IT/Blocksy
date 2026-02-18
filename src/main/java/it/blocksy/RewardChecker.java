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
            List<BlocksyReward> rewards = api.fetchRewards(apiKey);
            if (rewards.isEmpty()) return;
            
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
                String cmd = reward.getCommand().replace("%player%", player.getName());
                plugin.getLogger().info("Eseguendo premio Ruota della Fortuna per " + player.getName() + ": " + cmd);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            } else {
                // Il player è offline, in Metodo A l'API ha già marcato come 'executed'.
                // Se volessimo ri-accodare dovremmo gestire il fallimento nell'API.
                // Per ora assumiamo che il plugin esegua se online o che l'owner gestisca il log.
                plugin.getLogger().info("Premio Ruota della Fortuna per " + reward.getUsername() + " ignorato (player offline).");
            }
        });
    }
}
