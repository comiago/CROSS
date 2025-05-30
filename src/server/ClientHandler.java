package server;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;

import model.Client;
import org.json.JSONException;
import org.json.JSONObject;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private static RequestController controller;
    private static Network network;
    private static Client client;

    public ClientHandler(Network network, Socket clientSocket, Client client) {
        this.clientSocket = clientSocket;
        network = network;
        controller = new RequestController(network);
        client = client;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String rawMessage;
            while ((rawMessage = in.readLine()) != null) {
                try {
                    JSONObject request = new JSONObject(rawMessage);
                    JSONObject response = processRequest(request); // Nuovo metodo
                    out.println(response.toString());
                } catch (JSONException e) {
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

    private JSONObject processRequest(JSONObject request) {
        JSONObject response = new JSONObject();
        try {
            String operation = request.getString("operation");
            JSONObject values = request.getJSONObject("values");

            switch (operation) {
                case "help":
                    response = controller.handleHelp(values);
                    break;
                case "register":
                    response = controller.handleRegister(values);
                    break;
                case "udpConnection":
                    response = controller.handleUdpConnection(client, values);
                    break;
                // Aggiungi altri casi qui...
                default: 
                    network.sendMessage(response, 103, "Operazione non supportata");
            }
        } catch (JSONException e) {
            network.sendMessage(response, 103, "Campi mancanti nel JSON");
        } catch (SocketException e) {
            network.sendMessage(response, 101, "Errore di connessione UDP");
        }
        return response;
    }
}
