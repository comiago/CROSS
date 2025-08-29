package controller;

import com.google.gson.JsonObject;
import model.*;
import server.Network;
import util.MessageBuilder;
import util.OrderStorage;
import util.Notifier;

import java.io.IOException;
import java.net.SocketException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller principale per la gestione delle richieste dei client.
 * Gestisce login, registrazione, aggiornamento credenziali e inserimento ordini.
 */
public class RequestController {

    private final Network network;
    private static MessageBuilder msgBuilder;
    private static UserController userController;
    private static Client client;
    private static OrderBook orderBook;
    private static Notifier notifier;

    // Messaggi di help per i comandi disponibili
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

    public RequestController(Network network, Client client, OrderBook orderBook, Notifier notifier) {
        this.network = network;
        msgBuilder = new MessageBuilder();
        userController = new UserController();
        RequestController.client = client;
        RequestController.orderBook = orderBook;
        RequestController.notifier = notifier;
    }

    /**
     * Gestisce il comando 'help'
     * @param request JSON contenente eventualmente il comando specifico
     * @return risposta JSON con le informazioni sui comandi
     */
    public JsonObject handleHelp(JsonObject request) {
        String command = request.has("command") && !request.get("command").isJsonNull()
                ? request.get("command").getAsString().trim()
                : "";

        if (command.isEmpty()) {
            // Mostra tutti i comandi
            StringBuilder allCommands = new StringBuilder("\n=== COMANDI DISPONIBILI ===\n");
            helpMessages.values().forEach(msg -> allCommands.append(msg).append("\n"));
            return msgBuilder.buildResponse(100, allCommands.toString().trim());
        }

        // Mostra info su comando specifico
        String help = helpMessages.get(command.toLowerCase());
        if (help != null) {
            return msgBuilder.buildResponse(100, "\n=== HELP: " + command + " ===\n" + help);
        } else {
            return msgBuilder.buildResponse(101, "Comando non riconosciuto: " + command + ". Scrivi 'help' per l'elenco completo.");
        }
    }

    /**
     * Gestisce la registrazione di un nuovo utente
     */
    public static JsonObject handleRegister(JsonObject request) {
        String username = getSafeString(request, "username");
        String password = getSafeString(request, "password");
        JsonObject response;

        if (username.isEmpty()) return msgBuilder.buildResponse(103, "Username vuoto");
        if (password.isEmpty()) return msgBuilder.buildResponse(101, "Password non valida");

        if (userController.registerUser(username, password)) {
            response = msgBuilder.buildResponse(100, "OK");
        } else {
            response = msgBuilder.buildResponse(102, "Username non disponibile");
        }
        return response;
    }

    /**
     * Gestisce il login di un utente esistente
     */
    public static JsonObject handleLogin(JsonObject request) {
        String username = getSafeString(request, "username");
        String password = getSafeString(request, "password");
        JsonObject response;

        if (username.isEmpty() || password.isEmpty()) {
            return msgBuilder.buildResponse(103, "Username o password mancanti");
        }

        if (userController.loginUser(username, password)) {
            response = msgBuilder.buildResponse(100, "OK");
        } else {
            response = msgBuilder.buildResponse(101, "Username/password non corrispondenti o inesistenti");
        }
        return response;
    }

    /**
     * Gestisce l'aggiornamento delle credenziali
     */
    public static JsonObject handleUpdateCredentials(JsonObject request) {
        String username = getSafeString(request, "username");
        String oldPassword = getSafeString(request, "oldPassword");
        String newPassword = getSafeString(request, "newPassword");

        if (username.isEmpty() || oldPassword.isEmpty()) {
            return msgBuilder.buildResponse(105, "Username o password vecchia mancanti");
        }
        if (newPassword.isEmpty()) {
            return msgBuilder.buildResponse(101, "Nuova password non valida");
        }
        if (oldPassword.equals(newPassword)) {
            return msgBuilder.buildResponse(103, "Nuova password uguale alla vecchia");
        }

        if (userController.updateCredentials(username, oldPassword, newPassword)) {
            return msgBuilder.buildResponse(100, "OK");
        } else {
            return msgBuilder.buildResponse(102, "Username/password non corrispondenti o inesistenti");
        }
    }

    /**
     * Gestisce la connessione UDP del client
     */
    public JsonObject handleUdpConnection(Client client, JsonObject request) throws SocketException {
        int port = request.has("port") && !request.get("port").isJsonNull()
                ? request.get("port").getAsInt()
                : 0;

        client.setUdpPort(port);
        return msgBuilder.buildResponse(100, "Connessione UDP avvenuta con successo");
    }

    /**
     * Inserimento di un LimitOrder
     */
    public JsonObject handleInsertLimitOrder(String clientName, JsonObject request) throws IOException {
        long orderId = orderBook.generateOrderId();
        LimitOrder limitOrder = new LimitOrder(
                orderId,
                clientName,
                Side.valueOf(request.get("type").getAsString()),
                request.get("size").getAsInt(),
                request.get("price").getAsInt()
        );

        List<ExecutedTrade> trades = orderBook.matchLimitOrder(limitOrder);

        // Notifica eventuali trade chiusi
        for (ExecutedTrade t : trades) {
            OrderStorage.appendToExecutedOrders(t);
            JsonObject tradesObj = buildTradeJson(t);
            notifier.notifyTrade(t.getBuyer(), t.getSeller(), msgBuilder.buildNotification("closedTrade", tradesObj));
        }

        // Salva pending orders
        OrderStorage.savePendingLimitOrders(orderBook);

        JsonObject response = new JsonObject();
        response.addProperty("orderId", orderId);
        return response;
    }

    /**
     * Inserimento di un MarketOrder
     */
    public JsonObject handleInsertMarketOrder(String clientName, JsonObject request) throws IOException {
        long orderId = orderBook.generateOrderId();
        MarketOrder marketOrder = new MarketOrder(
                orderId,
                clientName,
                Side.valueOf(request.get("type").getAsString()),
                request.get("size").getAsInt()
        );

        List<ExecutedTrade> trades = orderBook.matchMarketOrder(marketOrder);

        for (ExecutedTrade t : trades) {
            OrderStorage.appendToExecutedOrders(t);
            JsonObject tradesObj = buildTradeJson(t);
            notifier.notifyTrade(t.getBuyer(), t.getSeller(), msgBuilder.buildNotification("closedTrade", tradesObj));
        }

        OrderStorage.savePendingLimitOrders(orderBook);

        JsonObject response = new JsonObject();
        response.addProperty("orderId", orderId);
        return response;
    }

    /**
     * Inserimento di uno StopOrder
     */
    public JsonObject handleInsertStopOrder(String clientName, JsonObject request) throws IOException {
        long orderId = orderBook.generateOrderId();
        StopOrder stopOrder = new StopOrder(
                orderId,
                clientName,
                Side.valueOf(request.get("type").getAsString()),
                request.get("size").getAsInt(),
                request.get("stopPrice").getAsInt()
        );

        orderBook.addStopOrder(stopOrder);
        OrderStorage.savePendingStopOrders(orderBook);

        JsonObject response = new JsonObject();
        response.addProperty("orderId", orderId);
        return response;
    }

    /**
     * Costruisce un JSON semplificato per notificare i trade
     */
    private JsonObject buildTradeJson(ExecutedTrade t) {
        JsonObject obj = new JsonObject();
        obj.addProperty("buyer", t.getBuyer());
        obj.addProperty("seller", t.getSeller());
        obj.addProperty("orderType", t.getOrderType().toString());
        obj.addProperty("side", t.getInitiatorSide().toString());
        obj.addProperty("size", t.getSize());
        obj.addProperty("price", t.getPrice());
        obj.addProperty("timestamp", t.getTimestamp());
        return obj;
    }

    /**
     * Estrae valori da JSON in sicurezza
     */
    private static String getSafeString(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull()
                ? obj.get(key).getAsString().trim()
                : "";
    }
}
