package it.blocksy.config;

import it.blocksy.Blocksy;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;

public class ConfigManager {
    
    private final Blocksy plugin;
    private String apiKey;
    private int checkInterval;
    
    public ConfigManager(Blocksy plugin) {
        this.plugin = plugin;
    }
    
    public void loadConfig() {
        FileConfiguration config = plugin.getConfig();
        
        this.apiKey = config.getString("api-key", "");
        this.checkInterval = config.getInt("check-interval", 5);
    }
    
    public String getApiKey() {
        return apiKey;
    }
    
    public int getCheckInterval() {
        return checkInterval;
    }
    
    /**
     * Ricarica la configurazione dal file config.yml
     */
    public void reloadConfig() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            plugin.getLogger().severe("Impossibile creare la cartella dati di Blocksy: " + dataFolder.getPath());
            return;
        }
        
        File configFile = new File(dataFolder, "config.yml");
        if (!configFile.exists()) {
            plugin.saveDefaultConfig();
        }
        
        plugin.reloadConfig();
        loadConfig();
    }
}
