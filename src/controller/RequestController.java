package controller;

import model.Client;
import com.google.gson.JsonObject;
import server.Network;
import util.MessageBuilder;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

public class RequestController {
    private static Network network;
    private static MessageBuilder msgBuilder;
    private static UserController userController;
    // Mappa con help specifico per ogni comando
    private static final Map<String, String> helpMessages = new HashMap<>();

    static {
        helpMessages.put("help", "help → Mostra questo messaggio o info su un comando (es: help login)");
        helpMessages.put("register", "register <user> <pwd> → Registra un nuovo account");
        helpMessages.put("login", "login <user> <pwd> → Accedi all'account");
        helpMessages.put("limit", "limit <bid/ask> <size> <price> → Inserisci limit order");
        helpMessages.put("market", "market <bid/ask> <size> → Inserisci market order");
        helpMessages.put("cancel", "cancel <orderID> → Cancella un ordine");
        helpMessages.put("logout", "logout → Disconnetti");
        helpMessages.put("exit", "exit → Chiudi il client");
    }

    public RequestController(Network network) {
        RequestController.network = network;
        msgBuilder = new MessageBuilder();
        userController = new UserController();
    }

    public static JsonObject handleHelp(JsonObject request) {
        JsonObject response;
        String command = "";

        if (request.has("command") && !request.get("command").isJsonNull()) {
            command = request.get("command").getAsString().trim();
        }

        if (command.isEmpty()) {
            // Help generico
            StringBuilder allCommands = new StringBuilder("\n=== COMANDI DISPONIBILI ===\n");
            for (String msg : helpMessages.values()) {
                allCommands.append(msg).append("\n");
            }
            response = msgBuilder.buildResponse(100, allCommands.toString().trim());
        } else {
            // Help specifico
            String help = helpMessages.get(command.toLowerCase());
            if (help != null) {
                response = msgBuilder.buildResponse(100, "\n=== HELP: " + command + " ===\n" + help);
            } else {
                response = msgBuilder.buildResponse(101, "Comando non riconosciuto: " + command + ". Scrivi 'help' per l'elenco completo.");
            }
        }

        return response;
    }

    public static JsonObject handleRegister(JsonObject request) {
        JsonObject response = new JsonObject();
        String username = "";
        String password = "";

        if (request.has("username") && !request.get("username").isJsonNull()) {
            username = request.get("username").getAsString();
        }
        if (request.has("password") && !request.get("password").isJsonNull()) {
            password = request.get("password").getAsString();
        }

        if (username.isEmpty()) {
            response = msgBuilder.buildResponse(103, "invalid password");
        }
        if (password.isEmpty()) {
            msgBuilder.buildResponse(103, "invalid username");
        }
        if(userController.registerUser(username, password)) {
            response = msgBuilder.buildResponse(100, "OK");
        } else {
            response = msgBuilder.buildResponse(102, "username not available");
        }
        return response;
    }

    public static JsonObject handleLogin(JsonObject request) {
        JsonObject response = new JsonObject();
        String username = "";
        String password = "";

        if (request.has("username") && !request.get("username").isJsonNull()) {
            username = request.get("username").getAsString();
        }
        if (request.has("password") && !request.get("password").isJsonNull()) {
            password = request.get("password").getAsString();
        }

        if (username.isEmpty()) {
            response = msgBuilder.buildResponse(103, "invalid password");
        }
        if (password.isEmpty()) {
            msgBuilder.buildResponse(103, "invalid username");
        }
        if(userController.loginUser(username, password)) {
            response = msgBuilder.buildResponse(100, "OK");
        } else {
            response = msgBuilder.buildResponse(101, "username/password mismatch or non existent username");
        }
        return response;
    }

    public static JsonObject handleUpdateCredentials(JsonObject request) {
        JsonObject response = new JsonObject();
        String username = "";
        String oldPassword = "";
        String newPassword = "";
        if (request.has("username") && !request.get("username").isJsonNull()) {
            username = request.get("username").getAsString();
        }
        if (request.has("oldPassword") && !request.get("oldPassword").isJsonNull()) {
            oldPassword = request.get("oldPassword").getAsString();
        }
        if (request.has("newPassword") && !request.get("newPassword").isJsonNull()) {
            newPassword = request.get("newPassword").getAsString();
        }

        if (username.isEmpty()) {
            response = msgBuilder.buildResponse(105, "invalid password");
        }
        if (oldPassword.isEmpty()) {
            msgBuilder.buildResponse(105, "invalid username");
        }
        if (newPassword.isEmpty()) {
            response = msgBuilder.buildResponse(101, "invalid new password");
        }
        if(oldPassword.equals(newPassword)) {
            response = msgBuilder.buildResponse(103, "new password equal to old one");
        }
        if(userController.updateCredentials(username, oldPassword, newPassword)) {
            response = msgBuilder.buildResponse(100, "OK");
        } else {
            response = msgBuilder.buildResponse(102, "username/old_password mismatch or non existent username");
        }
        return response;
    }

    public static JsonObject handleUdpConnection(Client client, JsonObject request) throws SocketException {
        JsonObject response = new JsonObject();
        int port = 0;
        if (request.has("port") && !request.get("port").isJsonNull()) {
            port = request.get("port").getAsInt();
        }
        client.setUdpPort(port);
        response.addProperty("response", 100);
        response.addProperty("errorMessage", "Connessione avvenuta con successo");
        return response;
    }
}
