package server;

import model.ServerConfig;
import util.ConfigFileManager;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ServerMain {
    private static ServerSocket socket;
    private static ExecutorService threadPool;

    public static void main(String[] args) {
        // Hook per la chiusura ordinata (CTRL+C)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            closeServerSocket();
            System.out.println("Server interrotto.");
        }));

        try {
            // Caricamento della configurazione
            ServerConfig config = ConfigFileManager.loadConfig("src/server/server.config", ServerConfig.class);
            System.out.println("Loaded config for server: " + config);

            // Inizializzazione del socket
            socket = new ServerSocket(
                    config.getServerPort(),
                    config.getBacklog(),
                    InetAddress.getByName(config.getServerAddress())
            );
            System.out.println("Server in ascolto su " + config.getServerAddress() + ":" + config.getServerPort());

            // Inizializzazione del thread pool
            threadPool = Executors.newFixedThreadPool(config.getMaxClients());


            // Loop principale per accettare connessioni
            while (true) {
                try {
                    Socket clientSocket = socket.accept();
                    System.out.println("Connessione stabilita con " + clientSocket.getInetAddress());

                    // Assegna il client a un thread del pool
                    threadPool.execute(new ClientHandler(clientSocket));
                } catch (IOException e) {
                    System.err.println("Errore durante l'accettazione della connessione: " + e.getMessage());
                }
            }

        } catch (IOException | IllegalAccessException | InstantiationException e) {
            System.err.println("Errore durante l'avvio del server: " + e.getMessage());
        } finally {
            closeServerSocket(); // Chiusura del server socket in caso di errore
            if (threadPool != null && !threadPool.isShutdown()) {
                threadPool.shutdown();
            }

        }
    }

    private static void closeServerSocket() {
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
