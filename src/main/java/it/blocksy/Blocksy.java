package it.blocksy;

import it.blocksy.config.ConfigManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class Blocksy extends JavaPlugin {
    
    private static Blocksy instance;
    private ConfigManager configManager;
    private VoteChecker voteChecker;
    private RewardChecker rewardChecker;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Inizializza i manager
        configManager = new ConfigManager(this);
        
        // Salva la configurazione di default
        saveDefaultConfig();
        configManager.loadConfig();
        
        // Avvia sistema di polling voti
        startVotePolling();
        
        // Registra comandi
        if (getCommand("blocksy") != null) {
            getCommand("blocksy").setTabCompleter(new BlocksyTabCompleter());
            getCommand("blocksy").setExecutor((sender, command, label, args) -> {
                if (!sender.hasPermission("blocksy.admin")) {
                    sender.sendMessage("§cNon hai i permessi per usare questo comando.");
                    return true;
                }
                
                if (args.length > 0) {
                    if (args[0].equalsIgnoreCase("reload")) {
                        reloadPolling();
                        sender.sendMessage("§a[Blocksy] Configurazione ricaricata e polling riavviato!");
                        return true;
                    }
                    
                    if (args[0].equalsIgnoreCase("endevent")) {
                        // Genera codice a 4 cifre casuale
                        int code = 1000 + (int)(Math.random() * 9000);
                        String codeStr = String.valueOf(code);

                        // Invia al sito
                        getServer().getScheduler().runTaskAsynchronously(this, () -> {
                            it.blocksy.api.BlocksyAPI apiClient = new it.blocksy.api.BlocksyAPI();
                            boolean success = apiClient.endEvent(configManager.getApiKey(), codeStr);
                            
                            getServer().getScheduler().runTask(this, () -> {
                                if (success) {
                                    String msg = configManager.getEventEndMessage().replace("%code%", codeStr).replace("&", "§");
                                    getServer().broadcastMessage(msg);
                                    // Rimosso sender.sendMessage per evitare "doppio messaggio" all'admin che vede già il broadcast
                                } else {
                                    sender.sendMessage("§c[Blocksy] Errore durante la chiusura dell'evento sul sito. Verifica i log o l'API Key.");
                                }
                            });
                        });
                        return true;
                    }
                }
                
                sender.sendMessage("§eUso: /blocksy <reload|endevent>");
                return true;
            });
        }
        
        getLogger().info("§aBlocksy Vote Plugin abilitato con successo!");
        getLogger().info("§aVersione: " + getDescription().getVersion());
        getLogger().info("§aAutore: " + getDescription().getAuthors().get(0));

        // Controllo aggiornamenti
        new it.blocksy.api.UpdateChecker(this).check();
    }

    @Override
    public void onDisable() {
        // Ferma i checker
        if (voteChecker != null) voteChecker.stop();
        if (rewardChecker != null) rewardChecker.stop();
        
        getLogger().info("§cBlocksy Vote Plugin disabilitato!");
    }
    
    /**
     * Avvia il sistema di polling voti dall'API
     */
    private void startVotePolling() {
        String apiKey = configManager.getApiKey();
        String backendId = configManager.getBackendId();
        int checkInterval = configManager.getCheckInterval();
        
        if (backendId == null || backendId.trim().isEmpty()) {
            backendId = getServer().getName();
        }
        
        if (apiKey.isEmpty()) {
            getLogger().warning("§c===========================================");
            getLogger().warning("§cAPI KEY NON CONFIGURATA!");
            getLogger().warning("§cConfigura 'api-key' in config.yml");
            getLogger().warning("§cIl sistema di voti non funzionerà!");
            getLogger().warning("§c===========================================");
            return;
        }
        
        getLogger().info("§eAvvio sistema di polling voti...");
        getLogger().info("§eAPI Key: " + apiKey.substring(0, Math.min(8, apiKey.length())) + "...");
        getLogger().info("§eBackend ID: " + backendId);
        getLogger().info("§eIntervallo controllo: " + checkInterval + " secondi");
        
        voteChecker = new VoteChecker(this, apiKey, backendId, checkInterval);
        voteChecker.start();

        // Avvia anche il sistema di polling premi CrazyTime
        rewardChecker = new RewardChecker(this, apiKey, backendId, checkInterval);
        rewardChecker.start();
    }
    
    public void reloadPolling() {
        if (voteChecker != null) voteChecker.stop();
        if (rewardChecker != null) rewardChecker.stop();
        
        configManager.reloadConfig();
        startVotePolling();
    }
    
    public static Blocksy getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public VoteChecker getVoteChecker() {
        return voteChecker;
    }
}
