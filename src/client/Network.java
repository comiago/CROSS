package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramSocket;
import java.net.Socket;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import util.MessageBuilder;

/**
 * Gestisce la connessione TCP/UDP del client al server.
 * Si occupa di inviare/ricevere messaggi JSON.
 */
public class Network {
    private final Socket tcpSocket;
    private final DatagramSocket udpSocket;
    private final BufferedReader in;
    private final PrintWriter out;
    private final MessageBuilder msgBuilder;

    /**
     * Costruisce la connessione TCP e UDP verso il server.
     *
     * @param address indirizzo server
     * @param port    porta server
     * @throws IOException se la connessione fallisce
     */
    public Network(String address, int port) throws IOException {
        msgBuilder = new MessageBuilder();
        try {
            // Apertura connessione TCP
            this.tcpSocket = new Socket(address, port);
            System.out.println("Connessione TCP stabilita con " + address + ":" + port);

            this.in = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));
            this.out = new PrintWriter(tcpSocket.getOutputStream(), true);

            // Creazione socket UDP locale
            this.udpSocket = new DatagramSocket();

            // Invia al server la porta UDP appena aperta
            JsonObject values = new JsonObject();
            values.addProperty("port", udpSocket.getLocalPort());
            sendJsonRequest(msgBuilder.buildRequest("udpConnection", values));
        } catch (IOException e) {
            System.err.println("Errore durante la connessione o I/O: " + e.getMessage());
            throw e; // Rilancia per gestione esterna
        }
    }

    /** @return il DatagramSocket UDP del client */
    public DatagramSocket getUdpSocket() {
        return udpSocket;
    }

    /**
     * Invia una richiesta JSON al server via TCP.
     *
     * @param request oggetto JSON da inviare
     * @throws IOException se l'invio fallisce
     */
    public void sendJsonRequest(JsonObject request) throws IOException {
        if (request == null) throw new IllegalArgumentException("Request JSON non può essere null");
        out.println(request.toString());
        if (out.checkError()) {
            throw new IOException("Errore nell'invio del messaggio al server");
        }
    }

    /**
     * Legge una risposta JSON dal server.
     *
     * @return JsonObject ricevuto, oppure null se la connessione è chiusa o JSON malformato
     * @throws IOException se la lettura fallisce
     */
    public JsonObject readJsonResponse() throws IOException {
        String line = in.readLine();
        if (line == null) return null; // Connessione chiusa

        try {
            return JsonParser.parseString(line).getAsJsonObject();
        } catch (JsonSyntaxException e) {
            System.err.println("JSON malformato ricevuto: " + line);
            return null;
        }
    }

    /** @return true se la connessione TCP è attiva */
    public boolean isConnected() {
        return tcpSocket != null && tcpSocket.isConnected() && !tcpSocket.isClosed();
    }

    /** Chiude TCP e UDP in modo sicuro */
    public void close() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (tcpSocket != null && !tcpSocket.isClosed()) {
                tcpSocket.close();
                System.out.println("Connessione TCP chiusa");
            }
            if (udpSocket != null && !udpSocket.isClosed()) {
                udpSocket.close();
                System.out.println("Socket UDP chiuso");
            }
        } catch (IOException e) {
            System.err.println("Errore durante la chiusura delle connessioni: " + e.getMessage());
        }
    }
}
