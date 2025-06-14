package controller;

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;

public class UserController {
    private static final String FILE_PATH = "src/server/users.json";

    // Carica utenti dal file
    public static JsonObject loadUsers() {
        try {
            if (!Files.exists(Paths.get(FILE_PATH))) {
                return new JsonObject(); // File non esiste → vuoto
            }

            String json = new String(Files.readAllBytes(Paths.get(FILE_PATH)));
            return JsonParser.parseString(json).getAsJsonObject();
        } catch (IOException | JsonSyntaxException e) {
            System.err.println("Errore nel caricamento utenti: " + e.getMessage());
            return new JsonObject(); // fallback vuoto
        }
    }

    // Salva utenti nel file
    public static void saveUsers(JsonObject users) {
        try (FileWriter writer = new FileWriter(FILE_PATH)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(users, writer);
        } catch (IOException e) {
            System.err.println("Errore nel salvataggio utenti: " + e.getMessage());
        }
    }

    // Aggiunge un nuovo utente se non esiste già
    public static boolean registerUser(String username, String password) {
        JsonObject users = loadUsers();

        if (users.has(username)) {
            return false; // utente già registrato
        }

        JsonObject userData = new JsonObject();
        userData.addProperty("password", password);

        users.add(username, userData);
        saveUsers(users);
        return true;
    }

    public static boolean loginUser(String username, String password) {
        JsonObject users = loadUsers();
        if (users.has(username)) {
            JsonObject userData = users.getAsJsonObject(username);
            if (userData.has("password") && userData.get("password").getAsString().equals(password)) {
                return true;
            }
        }
        return false;
    }

    public static boolean updateCredentials(String username, String oldPassword, String newPassword) {
        JsonObject users = loadUsers();
        if (users.has(username)) {
            JsonObject userData = users.getAsJsonObject(username);
            if (userData.has("password") && userData.get("password").getAsString().equals(oldPassword)) {
                userData.addProperty("password", newPassword);
                users.add(username, userData);
                saveUsers(users);
                return true;
            }
        }
        return false;
    }
}
