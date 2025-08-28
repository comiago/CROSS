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

public class RequestController {

    private final Network network;
    private static MessageBuilder msgBuilder;
    private static UserController userController;
    private static Client client;
    private static OrderBook orderBook;
    private static Notifier notifier;

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
        this.msgBuilder = new MessageBuilder();
        this.userController = new UserController();
        this.client = client;
        this.orderBook = orderBook;
        this.notifier = notifier;
    }

    public JsonObject handleHelp(JsonObject request) {
        String command = request.has("command") && !request.get("command").isJsonNull()
                ? request.get("command").getAsString().trim()
                : "";

        if (command.isEmpty()) {
            StringBuilder allCommands = new StringBuilder("\n=== COMANDI DISPONIBILI ===\n");
            helpMessages.values().forEach(msg -> allCommands.append(msg).append("\n"));
            return msgBuilder.buildResponse(100, allCommands.toString().trim());
        }

        String help = helpMessages.get(command.toLowerCase());
        if (help != null) {
            return msgBuilder.buildResponse(100, "\n=== HELP: " + command + " ===\n" + help);
        } else {
            return msgBuilder.buildResponse(101, "Comando non riconosciuto: " + command + ". Scrivi 'help' per l'elenco completo.");
        }
    }

    public static JsonObject handleRegister(JsonObject request) {
        JsonObject response;
        String username = "";
        String password = "";

        if (request.has("username") && !request.get("username").isJsonNull()) {
            username = request.get("username").getAsString();
        }
        if (request.has("password") && !request.get("password").isJsonNull()) {
            password = request.get("password").getAsString();
        }

        if (username.isEmpty()) {
            response = msgBuilder.buildResponse(103, "altri errori");
            return response;
        }
        if (password.isEmpty()) {
            response = msgBuilder.buildResponse(101, "password non valida");
            return response;
        }

        if(userController.registerUser(username, password)) {
            response = msgBuilder.buildResponse(100, "OK");
        } else {
            response = msgBuilder.buildResponse(102, "username non disponibile");
        }

        return response;
    }


    public static JsonObject handleLogin(JsonObject request) {
        JsonObject response;
        String username = "";
        String password = "";

        if (request.has("username") && !request.get("username").isJsonNull()) {
            username = request.get("username").getAsString();
        }
        if (request.has("password") && !request.get("password").isJsonNull()) {
            password = request.get("password").getAsString();
        }

        if (username.isEmpty() || password.isEmpty()) {
            response = msgBuilder.buildResponse(103, "altri errori");
            return response;
        }

        if(userController.loginUser(username, password)) {
            response = msgBuilder.buildResponse(100, "OK");
        } else {
            response = msgBuilder.buildResponse(101, "username/password non corrispondenti o inesistenti");
        }

        return response;
    }


    public static JsonObject handleUpdateCredentials(JsonObject request) {
        JsonObject response;
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

        if (username.isEmpty() || oldPassword.isEmpty()) {
            response = msgBuilder.buildResponse(105, "empty username or password");
            return response;
        }
        if (newPassword.isEmpty()) {
            response = msgBuilder.buildResponse(101, "nuova password non valida");
            return response;
        }
        if (oldPassword.equals(newPassword)) {
            response = msgBuilder.buildResponse(103, "nuova password uguale alla vecchia");
            return response;
        }

        if(userController.updateCredentials(username, oldPassword, newPassword)) {
            response = msgBuilder.buildResponse(100, "OK");
        } else {
            response = msgBuilder.buildResponse(102, "username/password non corrispondenti o inesistenti");
        }

        return response;
    }


    public JsonObject handleUdpConnection(Client client, JsonObject request) throws SocketException {
        int port = request.has("port") && !request.get("port").isJsonNull()
                ? request.get("port").getAsInt()
                : 0;

        client.setUdpPort(port);
        return msgBuilder.buildResponse(100, "Connessione avvenuta con successo");
    }

    public JsonObject handleInsertLimitOrder(String client, JsonObject request) throws IOException {
        JsonObject response = new JsonObject();
        long orderId = orderBook.generateOrderId();
        LimitOrder limitOrder = new LimitOrder(
                orderId,
                client,
                Side.valueOf(request.get("type").getAsString()),
                request.get("size").getAsInt(),
                request.get("price").getAsInt()
        );

        List<ExecutedTrade> trades = orderBook.matchLimitOrder(limitOrder);

        if (!trades.isEmpty()) {
            for (ExecutedTrade t : trades) {
                OrderStorage.appendToExecutedOrders(t);
                JsonObject tradesObj = new JsonObject();
                tradesObj.addProperty("buyer", t.getBuyer());
                tradesObj.addProperty("seller", t.getSeller());
                tradesObj.addProperty("orderType", t.getOrderType().toString());
                tradesObj.addProperty("side", t.getInitiatorSide().toString());
                tradesObj.addProperty("size", t.getSize());
                tradesObj.addProperty("price", t.getPrice());
                tradesObj.addProperty("timestamp", t.getTimestamp());
                notifier.notifyTrade(t.getBuyer(), t.getSeller(), msgBuilder.buildNotification("closedTrade", tradesObj));
            }
        }
        OrderStorage.savePendingLimitOrders(orderBook);
        
        response.addProperty("orderId", orderId);
        return response;
    }

    public JsonObject handleInsertMarketOrder(String client, JsonObject request) throws IOException {
        JsonObject response = new JsonObject();

        System.out.println(orderBook.getLimitAsks() + " " + orderBook.getLimitBids());

        long orderId = orderBook.generateOrderId();
        MarketOrder marketOrder = new MarketOrder(
                orderId,
                client,
                Side.valueOf(request.get("type").getAsString()),
                request.get("size").getAsInt()
        );

        List<ExecutedTrade> trades = orderBook.matchMarketOrder(marketOrder);

        if (!trades.isEmpty()) {
            for (ExecutedTrade t : trades) {
                OrderStorage.appendToExecutedOrders(t);

                JsonObject tradesObj = new JsonObject();
                tradesObj.addProperty("buyer", t.getBuyer());
                tradesObj.addProperty("seller", t.getSeller());
                tradesObj.addProperty("orderType", t.getOrderType().toString());
                tradesObj.addProperty("side", t.getInitiatorSide().toString());
                tradesObj.addProperty("size", t.getSize());
                tradesObj.addProperty("price", t.getPrice());
                tradesObj.addProperty("timestamp", t.getTimestamp());
                notifier.notifyTrade(t.getBuyer(), t.getSeller(), msgBuilder.buildNotification("closedTrade", tradesObj));
            }
        }

        // 👇 QUI aggiorni anche il file dei pending
        OrderStorage.savePendingLimitOrders(orderBook);

        response.addProperty("orderId", orderId);
        return response;
    }

    public JsonObject handleInsertStopOrder(String client, JsonObject request) throws IOException {
        long orderId = orderBook.generateOrderId();
        StopOrder stopOrder = new StopOrder(
                orderId,
                client,
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


    // Utilità per estrarre valori da un JSON in sicurezza
    private String getSafeString(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull()
                ? obj.get(key).getAsString().trim()
                : "";
    }
}
