package server;

import model.Client;
import org.json.JSONObject;

import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

public class RequestController {
    private static Network network;

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
        this.network = network;
    }

    public static JSONObject handleHelp(JSONObject request) {
        JSONObject response = new JSONObject();
        String command = request.optString("command", "").trim();

        if (command.isEmpty()) {
            // Help generico
            StringBuilder allCommands = new StringBuilder("\n=== COMANDI DISPONIBILI ===\n");
            for (String msg : helpMessages.values()) {
                allCommands.append(msg).append("\n");
            }
            response.put("response", 100);
            response.put("errorMessage", allCommands.toString().trim());
        } else {
            // Help specifico
            String help = helpMessages.get(command.toLowerCase());
            if (help != null) {
                response.put("response", 100);
                response.put("errorMessage", "\n=== HELP: " + command + " ===\n" + help);
            } else {
                response.put("response", 101);
                response.put("errorMessage", "Comando non riconosciuto: " + command + ". Scrivi 'help' per l'elenco completo.");
            }
        }

        return response;
    }

    public static JSONObject handleRegister(JSONObject request) {
        JSONObject response = new JSONObject();
        String username = request.getString("username");
        String password = request.getString("password");

        if (password.isEmpty()) {
            network.sendMessage(response, 101, "Password vuota");
        }
        // Altri controlli/commentati da implementare eventualmente

        return response;
    }

    public static JSONObject handleUdpConnection(Client client, JSONObject request) throws SocketException {
        JSONObject response = new JSONObject();
        int port = request.getInt("port");
        client.udpConnect(port);
        response.put("response", 100);
        response.put("errorMessage", "Connessione avvenuta con successo");
        return response;
    }
}
