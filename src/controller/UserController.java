package controller;

import com.google.gson.*;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;

/**
 * Controller per la gestione degli utenti lato server.
 * Permette registrazione, login e aggiornamento credenziali.
 * Le password devono essere già hashate prima di essere passate ai metodi.
 */
public class UserController {

    private final Path filePath;  // Percorso del file JSON utenti
    private final Gson gson;       // Gson con formattazione leggibile

    public UserController() {
        this.filePath = Paths.get("src/server/users.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    /**
     * Carica tutti gli utenti dal file JSON.
     * @return JsonObject con utenti, vuoto se il file non esiste o errore.
     */
    private synchronized JsonObject loadUsers() {
        try {
            if (!Files.exists(filePath)) {
                return new JsonObject(); // Nessun file, ritorna vuoto
            }
            String json = Files.readString(filePath);
            return JsonParser.parseString(json).getAsJsonObject();
        } catch (IOException | JsonSyntaxException e) {
            System.err.println("Errore nel caricamento utenti: " + e.getMessage());
            return new JsonObject();
        }
    }

    /**
     * Salva tutti gli utenti nel file JSON.
     * @param users JsonObject contenente tutti gli utenti
     */
    private synchronized void saveUsers(JsonObject users) {
        try (FileWriter writer = new FileWriter(filePath.toFile())) {
            gson.toJson(users, writer);
        } catch (IOException e) {
            System.err.println("Errore nel salvataggio utenti: " + e.getMessage());
        }
    }

    /**
     * Registra un nuovo utente. Restituisce false se username già esistente.
     * @param username nome utente
     * @param hashedPassword password già hashata
     */
    public boolean registerUser(String username, String hashedPassword) {
        JsonObject users = loadUsers();

        if (users.has(username)) {
            return false; // utente già esistente
        }

        JsonObject userData = new JsonObject();
        userData.addProperty("password", hashedPassword);

        users.add(username, userData);
        saveUsers(users);
        return true;
    }

    /**
     * Verifica login utente.
     * @param username nome utente
     * @param hashedPassword password hashata
     * @return true se username esiste e password corretta
     */
    public boolean loginUser(String username, String hashedPassword) {
        JsonObject users = loadUsers();

        if (users.has(username)) {
            JsonObject userData = users.getAsJsonObject(username);
            String stored = userData.get("password").getAsString();
            return stored.equals(hashedPassword);
        }

        return false;
    }

    /**
     * Aggiorna la password di un utente.
     * @param username nome utente
     * @param oldHashed password vecchia hashata
     * @param newHashed nuova password hashata
     * @return true se aggiornamento avvenuto con successo
     */
    public boolean updateCredentials(String username, String oldHashed, String newHashed) {
        JsonObject users = loadUsers();

        if (users.has(username)) {
            JsonObject userData = users.getAsJsonObject(username);
            String stored = userData.get("password").getAsString();

            if (stored.equals(oldHashed)) {
                userData.addProperty("password", newHashed);
                users.add(username, userData);
                saveUsers(users);
                return true;
            }
        }

        return false;
    }
}
