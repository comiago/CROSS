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

public class Network {
    private final Socket tcpSocket;
    private final DatagramSocket udpSocket;
    private final BufferedReader in;
    private final PrintWriter out;
    private final MessageBuilder msgBuilder;

    public Network(String address, int port) throws IOException {
        msgBuilder = new MessageBuilder();
        try {
            this.tcpSocket = new Socket(address, port);
            System.out.println("Connessione stabilita con " + address + ":" + port);

            this.in = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));
            this.out = new PrintWriter(tcpSocket.getOutputStream(), true);

            this.udpSocket = new DatagramSocket();

            // Invia la porta UDP appena aperta al server
            JsonObject values = new JsonObject();
            values.addProperty("port", udpSocket.getLocalPort());
            sendJsonRequest(msgBuilder.buildRequest("udpConnection", values));
        } catch (IOException e) {
            System.err.println("Errore durante la connessione o I/O: " + e.getMessage());
            throw e; // Rilancia per gestione esterna
        }
    }

    public DatagramSocket getUdpSocket() {
        return udpSocket;
    }

    public void sendJsonRequest(JsonObject request) throws IOException {
        if (request == null) throw new IllegalArgumentException("Request JSON non può essere null");
        String jsonString = request.toString();
        out.println(jsonString);
        if (out.checkError()) {
            throw new IOException("Errore nell'invio del messaggio al server");
        }
    }

    public JsonObject readJsonResponse() throws IOException {
        String line = in.readLine();
        if (line == null) {
            return null; // Connessione chiusa dal server
        }
        try {
            return JsonParser.parseString(line).getAsJsonObject();
        } catch (JsonSyntaxException e) {
            System.err.println("JSON malformato ricevuto: " + line);
            return null;
        }
    }

    public boolean isConnected() {
        return tcpSocket != null && tcpSocket.isConnected() && !tcpSocket.isClosed();
    }

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
