package server;

import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.io.IOException;
import java.net.Socket;
import com.google.gson.JsonObject;

public class Network {
    private static ServerSocket socket;

    public Network(int port, int backlog, String address) throws UnknownHostException {
        try {
            // Inizializzazione del socket
            socket = new ServerSocket(port, backlog, InetAddress.getByName(address));
            System.out.println("Server in ascolto su " + address + ":" + port);
        } catch (IOException e) {
            System.err.println("Errore durante l'avvio del server: " + e.getMessage());
        }
    }

    public static Socket accept() throws IOException {
        try {
            Socket clientSocket = socket.accept();
            System.out.println("Connessione stabilita con " + clientSocket.getInetAddress());
            return clientSocket;
        } catch (IOException e) {
            System.err.println("Errore durante l'accettazione della connessione: " + e.getMessage());
            return null;
        }
    }

    public void sendMessage(PrintWriter out, int code, String message) {
        JsonObject error = new JsonObject();
        error.addProperty("response", code);
        error.addProperty("errorMessage", message);
        out.println(error.toString());
    }

    public void sendMessage(JsonObject response, int code, String message) {
        response.addProperty("response", code);
        response.addProperty("errorMessage", message);
    }

    public static void closeServerSocket() {
        if (socket != null && !socket.isClosed()) {
            try {
                System.out.println("Chiusura del server socket...");
                socket.close();
            } catch (IOException e) {
                System.err.println("Errore durante la chiusura del server socket: " + e.getMessage());
            }
        }
    }
}
