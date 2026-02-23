package it.blocksy;

import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;
import it.blocksy.api.BlocksyAPI;
import it.blocksy.api.BlocksyVote;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

/**
 * Sistema di polling per recuperare voti dall'API
 * Simile a VoteChecker di MinecraftITALIA
 */
public class VoteChecker {
    
    private final Blocksy plugin;
    private final BlocksyAPI api;
    private final String apiKey;
    private final String backendId;
    private final int checkInterval;
    private BukkitTask task;
    private boolean running;
    private String lastError = null;
    private int consecutiveErrors = 0;
    
    public VoteChecker(Blocksy plugin, String apiKey, String backendId, int checkInterval) {
        this.plugin = plugin;
        this.api = new BlocksyAPI();
        this.apiKey = apiKey;
        this.backendId = backendId;
        this.checkInterval = checkInterval;
        this.running = false;
    }
    
    /**
     * Avvia il loop di controllo voti
     */
    public void start() {
        if (running) {
            return;
        }
        
        running = true;
        plugin.getLogger().info("Avvio sistema di polling voti...");
        plugin.getLogger().info("Controllo ogni " + checkInterval + " secondi");
        
        // Converti secondi in ticks (20 ticks = 1 secondo)
        long intervalTicks = checkInterval * 20L;
        
        // Verifica se siamo su Folia
        boolean isFolia = false;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionSerializer");
            isFolia = true;
        } catch (ClassNotFoundException ignored) {}

        if (isFolia) {
            plugin.getLogger().info("Rilevato ambiente Folia. Utilizzo GlobalRegionScheduler.");
            Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, (task) -> {
                checkForVotes();
            }, 20L, intervalTicks);
        } else {
            // Avvia task asincrono ripetuto standard per Bukkit/Paper
            task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
                checkForVotes();
            }, 20L, intervalTicks);
        }
    }
    
    /**
     * Ferma il loop di controllo voti
     */
    public void stop() {
        if (!running) {
            return;
        }
        
        running = false;
        if (task != null) {
            task.cancel();
            task = null;
        }
        plugin.getLogger().info("Sistema di polling voti fermato");
    }
    
    /**
     * Controlla se ci sono nuovi voti
     */
    private void checkForVotes() {
        try {
            // Recupera voti dall'API per questo backend
            List<BlocksyVote> votes = api.fetchVotes(apiKey, backendId);
            
            if (votes.isEmpty()) {
                resetErrors();
                return; // Nessun voto pendente
            }
            
            resetErrors();
            
            plugin.getLogger().info("Trovati " + votes.size() + " voti pendenti");
            
            // Processa ogni voto
            for (BlocksyVote vote : votes) {
                processVote(vote);
            }
            
        } catch (Exception e) {
            String currentError = e.getMessage();
            if (currentError == null) currentError = e.getClass().getName();
            
            // Logga l'errore solo se è diverso dal precedente o ogni 10 volte
            if (!currentError.equals(lastError) || consecutiveErrors % 10 == 0) {
                plugin.getLogger().warning("Errore nel controllo voti: " + currentError + (consecutiveErrors > 0 ? " (consecutivi: " + consecutiveErrors + ")" : ""));
                lastError = currentError;
            }
            consecutiveErrors++;
        }
    }
    
    /**
     * Resetta lo stato degli errori dopo un successo
     */
    private void resetErrors() {
        if (consecutiveErrors > 0) {
            plugin.getLogger().info("Connessione API ripristinata con successo dopo " + consecutiveErrors + " errori.");
            consecutiveErrors = 0;
            lastError = null;
        }
    }
    
    /**
     * Processa un singolo voto
     */
    private void processVote(BlocksyVote vote) {
        try {
            plugin.getLogger().info("Processando voto: " + vote.getUsername() + " (ID: " + vote.getId() + ")");
            
            // Crea un oggetto Vote di NuVotifier
            Vote votifierVote = new Vote();
            votifierVote.setUsername(vote.getUsername());
            votifierVote.setServiceName("Blocksy");
            votifierVote.setAddress("blocksy.it");
            votifierVote.setTimeStamp(vote.getTimestamp());
            
            // Verifica se siamo su Folia
            boolean isFolia = false;
            try {
                Class.forName("io.papermc.paper.threadedregions.RegionSerializer");
                isFolia = true;
            } catch (ClassNotFoundException ignored) {}

            Runnable eventTask = () -> {
                try {
                    VotifierEvent event = new VotifierEvent(votifierVote);
                    Bukkit.getPluginManager().callEvent(event);
                    plugin.getLogger().info("✓ Voto inviato a Votifier per " + vote.getUsername());
                } catch (Exception e) {
                    plugin.getLogger().severe("✗ Errore nell'invio evento Votifier: " + e.getMessage());
                }
            };

            if (isFolia) {
                Bukkit.getGlobalRegionScheduler().execute(plugin, eventTask);
            } else {
                // Invia l'evento Votifier nel thread principale standard
                Bukkit.getScheduler().runTask(plugin, eventTask);
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("Errore nel processare voto ID " + vote.getId() + ": " + e.getMessage());
        }
    }
    
    public boolean isRunning() {
        return running;
    }

    public String getApiKey() {
        return apiKey;
    }

    public int getCheckInterval() {
        return checkInterval;
    }
}
