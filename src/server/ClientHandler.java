package server;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import controller.RequestController;
import model.Client;
import model.Order;
import model.OrderBook;
import util.Colors;
import util.MessageBuilder;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;

public class ClientHandler implements Runnable {

    private final Socket clientSocket;
    private final Network network;
    private final Client client;
    private final RequestController controller;
    private final MessageBuilder msgBuilder;
    private static OrderBook orderBook;

    private boolean logged = false;

    public ClientHandler(Network network, Socket clientSocket, Client client, OrderBook orderBook) {
        this.network = network;
        this.clientSocket = clientSocket;
        this.client = client;
        this.orderBook = orderBook;
        this.controller = new RequestController(network, client, orderBook);
        this.msgBuilder = new MessageBuilder();
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String rawMessage;
            while ((rawMessage = in.readLine()) != null) {
                try {
                    JsonObject request = JsonParser.parseString(rawMessage).getAsJsonObject();
                    logClientMessage(request);

                    JsonObject response = processRequest(request);
                    logServerMessage(response);

                    out.println(response.toString());

                } catch (JsonSyntaxException e) {
                    network.sendJsonResponse(out, 103, "Formato JSON non valido");
                }
            }

        } catch (IOException e) {
            System.err.println("Errore di comunicazione con il client: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Errore durante la chiusura della socket client: " + e.getMessage());
            }
        }
    }

    private JsonObject processRequest(JsonObject request) {
        JsonObject response;

        try {
            String operation = request.get("operation").getAsString();
            JsonObject values = request.getAsJsonObject("values");

            switch (operation) {
                case "help":
                    response = controller.handleHelp(values);
                    break;
                case "udpConnection":
                    response = controller.handleUdpConnection(client, values);
                    break;
                case "register":
                    response = controller.handleRegister(values);
                    break;
                case "login":
                    response = handleLogin(values);
                    break;
                case "logout":
                    response = handleLogout();
                    break;
                case "updateCredentials":
                    response = handleUpdateCredentials(values);
                    break;
                case "insertLimitOrder":
                    response = handleInsertLimitOrder(values);
                    break;
                default:
                    response = msgBuilder.buildResponse(103, "Operazione non supportata");
            }

        } catch (NullPointerException | IllegalStateException e) {
            response = msgBuilder.buildResponse(103, "Campi mancanti o malformati nel JSON");
        } catch (SocketException e) {
            response = msgBuilder.buildResponse(101, "Errore di connessione UDP");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return response;
    }

    private JsonObject handleLogin(JsonObject values) {
        if (logged) {
            return msgBuilder.buildResponse(102, "Utente già loggato");
        }

        JsonObject response = controller.handleLogin(values);
        if (response.get("response").getAsInt() == 100) {
            logged = true;
            client.setUsername(values.get("username").getAsString());
        }

        return response;
    }

    private JsonObject handleLogout() {
        if (!logged) {
            return msgBuilder.buildResponse(101, "Utente non loggato");
        }

        logged = false;
        client.setUsername("user" + client.getId());
        return msgBuilder.buildResponse(100, "OK");
    }

    private JsonObject handleUpdateCredentials(JsonObject values) {
        if (logged) {
            return msgBuilder.buildResponse(104, "Impossibile aggiornare credenziali: utente loggato");
        }

        return controller.handleUpdateCredentials(values);
    }

    private JsonObject handleInsertLimitOrder(JsonObject values) throws IOException {
        if (!logged) {
            JsonObject response = new JsonObject();
            response.addProperty("orderId", -1);
            return response;
        }
        return controller.handleInsertLimitOrder(client, values);
    }

    private void logClientMessage(JsonObject request) {
        System.out.println(Colors.CYAN + "[" + client.getUsername() + "] " + request.toString() + Colors.RESET);
    }

    private void logServerMessage(JsonObject response) {
        System.out.println(Colors.BLUE + "[SERVER] " + response.toString() + Colors.RESET);
    }
}
