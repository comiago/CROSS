
package controller;

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;

public class UserController {

    private final Path filePath;
    private final Gson gson;

    public UserController() {
        this.filePath = Paths.get("src/server/users.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    private synchronized JsonObject loadUsers() {
        try {
            if (!Files.exists(filePath)) {
                return new JsonObject();
            }
            String json = Files.readString(filePath);
            return JsonParser.parseString(json).getAsJsonObject();
        } catch (IOException | JsonSyntaxException e) {
            System.err.println("Errore nel caricamento utenti: " + e.getMessage());
            return new JsonObject();
        }
    }

    private synchronized void saveUsers(JsonObject users) {
        try (FileWriter writer = new FileWriter(filePath.toFile())) {
            gson.toJson(users, writer);
        } catch (IOException e) {
            System.err.println("Errore nel salvataggio utenti: " + e.getMessage());
        }
    }

    // ⚠️ Le password sono già hashate
    public boolean registerUser(String username, String hashedPassword) {
        JsonObject users = loadUsers();

        if (users.has(username)) {
            return false; // utente già esistente
        }

        JsonObject userData = new JsonObject();
        userData.addProperty("password", hashedPassword); // salva direttamente

        users.add(username, userData);
        saveUsers(users);
        return true;
    }

    public boolean loginUser(String username, String hashedPassword) {
        JsonObject users = loadUsers();

        if (users.has(username)) {
            JsonObject userData = users.getAsJsonObject(username);
            String stored = userData.get("password").getAsString();
            return stored.equals(hashedPassword); // confronto diretto
        }

        return false;
    }

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
