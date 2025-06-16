package server;

import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class Network {

    private ServerSocket serverSocket;

    public Network(int port, int backlog, String address) throws UnknownHostException, IOException {
        try {
            InetAddress inetAddress = InetAddress.getByName(address);
            this.serverSocket = new ServerSocket(port, backlog, inetAddress);
            System.out.printf("Server in ascolto su %s:%d%n", address, port);
        } catch (IOException e) {
            System.err.println("Errore durante l'avvio del server: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Accetta una nuova connessione.
     *
     * @return Socket del client connesso
     * @throws IOException se avviene un errore durante l'accettazione
     */
    public Socket accept() throws IOException {
        Socket clientSocket = serverSocket.accept();
        System.out.println("Connessione stabilita con " + clientSocket.getInetAddress());
        return clientSocket;
    }

    /**
     * Invia un messaggio JSON formattato tramite PrintWriter.
     *
     * @param out     il writer del client
     * @param code    codice di risposta
     * @param message messaggio da inviare
     */
    public void sendJsonResponse(PrintWriter out, int code, String message) {
        JsonObject json = new JsonObject();
        json.addProperty("response", code);
        json.addProperty("errorMessage", message);
        out.println(json.toString());
    }

    /**
     * Aggiunge proprietà a un oggetto JSON di risposta.
     *
     * @param response oggetto JSON esistente
     * @param code     codice di risposta
     * @param message  messaggio di errore o informazione
     */
    public void enrichJsonResponse(JsonObject response, int code, String message) {
        response.addProperty("response", code);
        response.addProperty("errorMessage", message);
    }

    /**
     * Chiude il server socket.
     */
    public void closeServerSocket() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                System.out.println("Chiusura del server socket...");
                serverSocket.close();
            } catch (IOException e) {
                System.err.println("Errore durante la chiusura del server socket: " + e.getMessage());
            }
        }
    }
}
