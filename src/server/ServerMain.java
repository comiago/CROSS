package server;

import model.ServerConfig;
import util.ConfigFileManager;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ServerMain {
    private static ExecutorService threadPool;
    private static Network network;

    public static void main(String[] args) {
        // Hook per la chiusura ordinata (CTRL+C)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            network.closeServerSocket();
            System.out.println("Server interrotto.");
        }));

        try {
            // Caricamento della configurazione
            ServerConfig config = ConfigFileManager.loadConfig("src/server/server.config", ServerConfig.class);
            System.out.println("Loaded config for server: " + config);

            int port = config.getServerPort();
            int backlog = config.getBacklog();
            String address = config.getServerAddress();
            network = new Network(port, backlog, address);

            // Inizializzazione del thread pool
            threadPool = Executors.newFixedThreadPool(config.getMaxClients());

            // Loop principale per accettare connessioni
            while (true) {
                Socket clientSocket = network.accept();
                threadPool.execute(new ClientHandler(network, clientSocket));
            }

        } catch (IOException | IllegalAccessException | InstantiationException e) {
            System.err.println("Errore durante l'avvio del server: " + e.getMessage());
        } finally {
            network.closeServerSocket(); // Chiusura del server socket in caso di errore
            if (threadPool != null && !threadPool.isShutdown()) {
                threadPool.shutdown();
            }

        }
    }
}
