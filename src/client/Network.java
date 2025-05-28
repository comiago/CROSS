package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import org.json.JSONObject;

public class Network {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    // Rimuovo static per avere istanze separate
    public Network(String address, int port) throws IOException {
        try {
            this.socket = new Socket(address, port);
            System.out.println("Connessione stabilita con " + address + ":" + port);

            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            System.err.println("Errore durante la connessione o I/O: " + e.getMessage());
            throw e; // Rilancia l'eccezione per gestione esterna
        }
    }

    public void sendJsonRequest(JSONObject request) throws IOException {
        String jsonString = request.toString();
        out.println(jsonString);
    }

    public String readMessage() throws IOException {
        return in.readLine();
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public void close() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) {
                socket.close();
                System.out.println("Connessione chiusa");
            }
        } catch (IOException e) {
            System.err.println("Errore durante la chiusura: " + e.getMessage());
        }
    }
}