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
    private Socket tcpSocket;
    private DatagramSocket udpSocket;
    private BufferedReader in;
    private PrintWriter out;
    private MessageBuilder msgBuilder;

    // Rimuovo static per avere istanze separate
    public Network(String address, int port) throws IOException {
        msgBuilder = new MessageBuilder();
        try {
            this.tcpSocket = new Socket(address, port);
            System.out.println("Connessione stabilita con " + address + ":" + port);

            this.in = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));
            this.out = new PrintWriter(tcpSocket.getOutputStream(), true);

            this.udpSocket = new DatagramSocket();
            JsonObject values = new JsonObject();
            values.addProperty("port", udpSocket.getLocalPort());
            sendJsonRequest(msgBuilder.buildRequest("udpConnection", values));
        } catch (IOException e) {
            System.err.println("Errore durante la connessione o I/O: " + e.getMessage());
            throw e; // Rilancia l'eccezione per gestione esterna
        }
    }

    public DatagramSocket getUdpSocket() {
        return udpSocket;
    }

    public void sendJsonRequest(JsonObject request) throws IOException {
        String jsonString = request.toString();
        out.println(jsonString);
    }

    public JsonObject readJsonResponse() throws IOException {
        String line = in.readLine();
        if (line == null) {
            return null;
        }
        try {
            return JsonParser.parseString(line).getAsJsonObject();
        } catch (JsonSyntaxException e) {
            System.err.println("JSON malformato: " + line);
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
                System.out.println("Connessione chiusa");
            }
        } catch (IOException e) {
            System.err.println("Errore durante la chiusura: " + e.getMessage());
        }
    }
}
