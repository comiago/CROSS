package server;

import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Classe responsabile della gestione della rete lato server.
 * Si occupa di:
 * - Creare il ServerSocket
 * - Accettare connessioni client
 * - Inviare risposte JSON ai client
 * - Chiudere correttamente il server
 */
public class Network {

    private final ServerSocket serverSocket;

    /**
     * Costruttore: inizializza il ServerSocket con indirizzo, porta e backlog.
     *
     * @param port    porta di ascolto
     * @param backlog numero massimo di connessioni pendenti
     * @param address indirizzo del server (es. "localhost")
     * @throws UnknownHostException se l'indirizzo non è valido
     * @throws IOException          se non è possibile creare il server
     */
    public Network(int port, int backlog, String address) throws UnknownHostException, IOException {
        InetAddress inetAddress = InetAddress.getByName(address);
        this.serverSocket = new ServerSocket(port, backlog, inetAddress);
        System.out.printf("Server in ascolto su %s:%d%n", address, port);
    }

    /**
     * Accetta una nuova connessione client.
     *
     * @return socket del client connesso
     * @throws IOException se avviene un errore durante l'accettazione
     */
    public Socket accept() throws IOException {
        Socket clientSocket = serverSocket.accept();
        System.out.println("Connessione stabilita con " + clientSocket.getInetAddress());
        return clientSocket;
    }

    /**
     * Invia una risposta JSON semplice al client.
     *
     * @param out     writer del client
     * @param code    codice di risposta (es. 100 = OK)
     * @param message messaggio descrittivo
     */
    public void sendJsonResponse(PrintWriter out, int code, String message) {
        JsonObject json = buildJsonResponse(code, message);
        out.println(json.toString());
    }

    /**
     * Aggiunge informazioni di risposta a un oggetto JSON esistente.
     *
     * @param response oggetto JSON da arricchire
     * @param code     codice di risposta
     * @param message  messaggio descrittivo
     */
    public void enrichJsonResponse(JsonObject response, int code, String message) {
        response.addProperty("response", code);
        response.addProperty("errorMessage", message);
    }

    /**
     * Costruisce un oggetto JSON standard per risposte.
     *
     * @param code    codice di risposta
     * @param message messaggio descrittivo
     * @return JsonObject pronto da inviare
     */
    private JsonObject buildJsonResponse(int code, String message) {
        JsonObject json = new JsonObject();
        json.addProperty("response", code);
        json.addProperty("errorMessage", message);
        return json;
    }

    /**
     * Chiude il server socket in modo sicuro.
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
