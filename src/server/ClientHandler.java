package server;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        System.out.println("Handler avviato per " + clientSocket.getInetAddress());

        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            String message;

            // Ascolto continuo dei messaggi del client
            while ((message = in.readLine()) != null) {
                System.out.println("Ricevuto dal client: " + message);
                
            }

        } catch (IOException e) {
            System.err.println("Errore nella comunicazione con il client: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
                System.out.println("Connessione con il client chiusa.");
            } catch (IOException e) {
                System.err.println("Errore nella chiusura della connessione: " + e.getMessage());
            }
        }
    }
}
