package server;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import controller.RequestController;
import model.Client;
import model.OrderBook;
import util.Colors;
import util.MessageBuilder;
import util.Notifier;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;

/**
 * Gestisce la comunicazione TCP con un singolo client.
 * Riceve richieste JSON, le passa al controller e invia risposte.
 * Si occupa anche dello stato di login e del logging dei messaggi.
 */
public class ClientHandler implements Runnable {

    private final Socket clientSocket;
    private final Network network;
    private final Client client;
    private final RequestController controller;
    private final MessageBuilder msgBuilder;
    private static OrderBook orderBook;
    private static Notifier notifier;

    private boolean logged = false;

    public ClientHandler(Network network, Socket clientSocket, Client client, OrderBook orderBook, Notifier notifier) {
        this.network = network;
        this.clientSocket = clientSocket;
        this.client = client;
        ClientHandler.orderBook = orderBook;
        ClientHandler.notifier = notifier;
        this.controller = new RequestController(network, client, orderBook, notifier);
        this.msgBuilder = new MessageBuilder();
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String rawMessage;
            while ((rawMessage = in.readLine()) != null) {
                handleRawMessage(rawMessage, out);
            }

        } catch (IOException e) {
            System.err.println("Errore di comunicazione con il client: " + e.getMessage());
        } finally {
            closeClientSocket();
        }
    }

    /**
     * Gestisce la ricezione di un messaggio grezzo dal client.
     */
    private void handleRawMessage(String rawMessage, PrintWriter out) {
        try {
            JsonObject request = JsonParser.parseString(rawMessage).getAsJsonObject();
            logClientMessage(request);

            JsonObject response = processRequest(request);
            logServerMessage(response);

            out.println(response.toString());

        } catch (JsonSyntaxException e) {
            network.sendJsonResponse(out, 103, "Formato JSON non valido");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Processa la richiesta JSON e ritorna una risposta JSON.
     */
    private JsonObject processRequest(JsonObject request) {
        try {
            String operation = request.get("operation").getAsString();
            JsonObject values = request.getAsJsonObject("values");

            return switch (operation) {
                case "help" -> controller.handleHelp(values);
                case "udpConnection" -> controller.handleUdpConnection(client, values);
                case "register" -> controller.handleRegister(values);
                case "login" -> handleLogin(values);
                case "logout" -> handleLogout();
                case "updateCredentials" -> handleUpdateCredentials(values);
                case "insertLimitOrder" -> handleInsertLimitOrder(values);
                case "insertMarketOrder" -> handleInsertMarketOrder(values);
                case "insertStopOrder" -> handleInsertStopOrder(values);
                default -> msgBuilder.buildResponse(103, "Operazione non supportata");
            };

        } catch (NullPointerException | IllegalStateException e) {
            return msgBuilder.buildResponse(103, "Campi mancanti o malformati nel JSON");
        } catch (SocketException e) {
            return msgBuilder.buildResponse(101, "Errore di connessione UDP");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // -------------------- HANDLER OPERAZIONI --------------------

    private JsonObject handleLogin(JsonObject values) {
        if (logged) return msgBuilder.buildResponse(102, "Utente già loggato");

        JsonObject response = controller.handleLogin(values);
        if (response.get("response").getAsInt() == 100) {
            logged = true;
            client.setUsername(values.get("username").getAsString());
        }
        return response;
    }

    private JsonObject handleLogout() {
        if (!logged) return msgBuilder.buildResponse(101, "Utente non loggato");

        logged = false;
        client.setUsername("user" + client.getId());
        return msgBuilder.buildResponse(100, "OK");
    }

    private JsonObject handleUpdateCredentials(JsonObject values) {
        if (logged) return msgBuilder.buildResponse(104, "Impossibile aggiornare credenziali: utente loggato");
        return controller.handleUpdateCredentials(values);
    }

    private JsonObject handleInsertLimitOrder(JsonObject values) throws IOException {
        return logged ? controller.handleInsertLimitOrder(client.getUsername(), values) : createOrderRejectedResponse();
    }

    private JsonObject handleInsertMarketOrder(JsonObject values) throws IOException {
        return logged ? controller.handleInsertMarketOrder(client.getUsername(), values) : createOrderRejectedResponse();
    }

    private JsonObject handleInsertStopOrder(JsonObject values) throws IOException {
        return logged ? controller.handleInsertStopOrder(client.getUsername(), values) : createOrderRejectedResponse();
    }

    /**
     * Risposta standard per ordini quando il client non è loggato.
     */
    private JsonObject createOrderRejectedResponse() {
        JsonObject response = new JsonObject();
        response.addProperty("orderId", -1);
        return response;
    }

    // -------------------- LOGGING --------------------

    private void logClientMessage(JsonObject request) {
        System.out.println(Colors.CYAN + "[" + client.getUsername() + "] " + request + Colors.RESET);
    }

    private void logServerMessage(JsonObject response) {
        System.out.println(Colors.BLUE + "[SERVER] " + response + Colors.RESET);
    }

    // -------------------- UTILITY --------------------

    private void closeClientSocket() {
        try {
            clientSocket.close();
        } catch (IOException e) {
            System.err.println("Errore durante la chiusura della socket client: " + e.getMessage());
        }
    }
}
