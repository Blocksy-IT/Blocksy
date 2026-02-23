package it.blocksy.api;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Client API per recuperare voti dal sito Blocksy
 * Simile a McItaAPI di MinecraftITALIA
 */
public class BlocksyAPI {
    
    private static final String API_URL = "https://www.blocksy.it/api/vote/fetch";
    private static final int TIMEOUT = 10000; // 10 secondi
    private final Gson gson;
    
    public BlocksyAPI() {
        this.gson = new Gson();
    }
    
    /**
     * Recupera i voti pendenti dall'API
     * @param apiKey La chiave API del server
     * @param backendId Identificatore univoco di questo backend
     * @return Lista di voti pendenti
     */
    public List<BlocksyVote> fetchVotes(String apiKey, String backendId) {
        HttpURLConnection connection = null;
        try {
            // Costruisci URL con parametri apiKey e backendId (se presente)
            StringBuilder urlBuilder = new StringBuilder(API_URL)
                    .append("?apiKey=").append(apiKey);
            
            if (backendId != null) {
                String trimmed = backendId.trim();
                if (!trimmed.isEmpty()) {
                    urlBuilder.append("&backendId=")
                              .append(URLEncoder.encode(trimmed, "UTF-8"));
                }
            }
            
            String urlString = urlBuilder.toString();
            URL url = new URL(urlString);
            
            // Apri connessione
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(TIMEOUT);
            connection.setReadTimeout(TIMEOUT);
            connection.setRequestProperty("User-Agent", "Blocksy-Plugin/1.0");
            connection.setRequestProperty("Accept", "application/json");
            
            // Controlla risposta
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                throw new Exception("HTTP " + responseCode + ": " + connection.getResponseMessage());
            }
            
            // Leggi risposta
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream())
            );
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            // Parse JSON
            Type listType = new TypeToken<List<BlocksyVote>>(){}.getType();
            List<BlocksyVote> votes = gson.fromJson(response.toString(), listType);
            
            return votes != null ? votes : new ArrayList<>();
            
        } catch (Exception e) {
            return new ArrayList<>();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Recupera i premi pendenti (CrazyTime) dall'API
     * @param apiKey La chiave API del server
     * @param onlinePlayers Lista dei nomi dei player online (separati da virgola)
     * @return Lista di premi pendenti
     */
    public List<BlocksyReward> fetchRewards(String apiKey, String onlinePlayers) {
        HttpURLConnection connection = null;
        try {
            String urlString = "https://www.blocksy.it/api_reward_fetch.php?apiKey=" + apiKey;
            if (onlinePlayers != null && !onlinePlayers.isEmpty()) {
                urlString += "&onlinePlayers=" + java.net.URLEncoder.encode(onlinePlayers, "UTF-8");
            }
            URL url = new URL(urlString);
            
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(TIMEOUT);
            connection.setReadTimeout(TIMEOUT);
            connection.setRequestProperty("User-Agent", "Blocksy-Plugin/1.0");
            
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) return new ArrayList<>();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
            reader.close();
            
            Type listType = new TypeToken<List<BlocksyReward>>(){}.getType();
            List<BlocksyReward> rewards = gson.fromJson(response.toString(), listType);
            
            return rewards != null ? rewards : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        } finally {
            if (connection != null) connection.disconnect();
        }
    }
}
