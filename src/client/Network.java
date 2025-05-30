package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketException;

import org.json.JSONException;
import org.json.JSONObject;

public class Network {
    private Socket tcpSocket;
    private DatagramSocket udpSocket;
    private BufferedReader in;
    private PrintWriter out;

    // Rimuovo static per avere istanze separate
    public Network(String address, int port) throws IOException {
        try {
            this.tcpSocket = new Socket(address, port);
            System.out.println("Connessione stabilita con " + address + ":" + port);

            this.in = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));
            this.out = new PrintWriter(tcpSocket.getOutputStream(), true);

            this.udpSocket = new DatagramSocket();
            JSONObject request = new JSONObject();
            JSONObject values = new JSONObject();
            request.put("operation", "udpConnection");
            values.put("port", udpSocket.getLocalPort());
            request.put("values", values);
            sendJsonRequest(request);
        } catch (IOException e) {
            System.err.println("Errore durante la connessione o I/O: " + e.getMessage());
            throw e; // Rilancia l'eccezione per gestione esterna
        }
    }

    public void sendJsonRequest(JSONObject request) throws IOException {
        String jsonString = request.toString();
        out.println(jsonString);
    }

    public JSONObject readJsonResponse() throws IOException {
        String line = in.readLine();
        if (line == null) {
            return null;
        }
        try {
            return new JSONObject(line);
        } catch (JSONException e) {
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