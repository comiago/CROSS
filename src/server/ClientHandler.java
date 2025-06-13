package server;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;

import util.Colors;
import util.MessageBuilder;
import controllers.RequestController;
import model.Client;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private static RequestController controller;
    private static Network network;
    private static Client client;
    private static MessageBuilder msgBuilder;
    private boolean logged = false;

    public ClientHandler(Network network, Socket clientSocket, Client client) {
        this.clientSocket = clientSocket;
        ClientHandler.network = network;
        controller = new RequestController(network);
        ClientHandler.client = client;
        msgBuilder = new MessageBuilder();
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String rawMessage;
            while ((rawMessage = in.readLine()) != null) {
                try {
                    JsonObject request = JsonParser.parseString(rawMessage).getAsJsonObject();
                    System.out.print(Colors.CYAN + "[" + client.getUsername() + "]" + request.toString() + Colors.RESET + "\n");
                    JsonObject response = processRequest(request); // Nuovo metodo
                    System.out.print(Colors.BLUE + "[SERVER]" + response.toString() + Colors.RESET + "\n");
                    out.println(response.toString());
                } catch (JsonSyntaxException e) {
                    network.sendMessage(out, 103, "Formato JSON non valido");
                }
            }
        } catch (IOException e) {
            System.err.println("Errore di comunicazione: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Errore chiusura socket: " + e.getMessage());
            }
        }
    }

    private JsonObject processRequest(JsonObject request) {
        JsonObject response = new JsonObject();
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
                    if(logged) response = msgBuilder.buildResponse(102, "user already logged in");
                    else response = controller.handleLogin(values);
                    if(response.get("response").getAsInt() == 100) {
                        logged = true;
                        client.setUsername(values.get("username").getAsString());
                    }
                    break;
                case "logout":
                    if(!logged) {
                        response = msgBuilder.buildResponse(101, "user not logged in");
                    } else {
                        logged = false;
                        client.setUsername("user" + client.getId());
                        response = msgBuilder.buildResponse(100, "OK");
                    }
                    break;
                case "updateCredentials":
                    if(logged) response = msgBuilder.buildResponse(104, "user currently logged in");
                    else response = controller.handleUpdateCredentials(values);
                    break;
                default:
                    response = msgBuilder.buildResponse(103, "Operazione non supportata");
            }
        } catch (NullPointerException | IllegalStateException e) {
            response = msgBuilder.buildResponse(103, "Campi mancanti o malformati nel JSON");
        } catch (SocketException e) {
            response = msgBuilder.buildResponse(101, "Errore di connessione UDP");
        }
        return response;
    }
}
