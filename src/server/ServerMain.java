package server;

import model.OrderBook;
import model.ServerConfig;
import model.Client;
import util.ConfigFileManager;
import util.Notifier;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Classe principale del server.
 * Gestisce:
 * - Caricamento configurazione
 * - Avvio del server TCP
 * - Accettazione e gestione dei client
 * - Gestione ordine e notifiche
 */
public class ServerMain {

    private static ExecutorService threadPool;
    private static Network network;
    private static final List<Client> clients = new ArrayList<>();
    private static final OrderBook orderBook = new OrderBook();
    private static Notifier notifier;

    public static void main(String[] args) {
        if (args.length != 0) {
            throw new IllegalArgumentException("Il server non accetta argomenti. Avviare senza parametri.");
        }

        addShutdownHook();

        try {
            // Carica configurazione dal file
            ServerConfig config = ConfigFileManager.loadConfig("src/config/server.config", ServerConfig.class);
            System.out.println("Configurazione caricata: " + config);

            // Avvia server TCP
            startServer(config);

            // Ciclo principale di accettazione client
            handleClients();

        } catch (IOException | ReflectiveOperationException e) {
            System.err.println("Errore durante l'avvio del server: " + e.getMessage());
            e.printStackTrace();
        } finally {
            shutdown();
        }
    }

    /**
     * Aggiunge un hook per chiudere correttamente il server in caso di terminazione.
     */
    private static void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            shutdown();
            System.out.println("Server interrotto.");
        }));
    }

    /**
     * Inizializza network, thread pool e notificatore.
     *
     * @param config configurazione del server
     * @throws IOException in caso di errore nell'avvio della socket
     */
    private static void startServer(ServerConfig config) throws IOException {
        network = new Network(config.getServerPort(), config.getBacklog(), config.getServerAddress());
        threadPool = Executors.newFixedThreadPool(config.getMaxClients());
        notifier = new Notifier(clients);

        System.out.printf("Server in ascolto su %s:%d (max %d client)%n",
                config.getServerAddress(), config.getServerPort(), config.getMaxClients());
    }

    /**
     * Ciclo principale per accettare connessioni client e avviare ClientHandler.
     *
     * @throws IOException se fallisce l'accettazione di un client
     */
    private static void handleClients() throws IOException {
        while (true) {
            Socket clientSocket = network.accept();
            Client client = new Client(clients.size(), clientSocket);
            clients.add(client);

            // Assegna il client handler al thread pool
            ClientHandler handler = new ClientHandler(network, clientSocket, client, orderBook, notifier);
            threadPool.execute(handler);
        }
    }

    /**
     * Termina correttamente server e thread pool.
     */
    private static void shutdown() {
        if (network != null) {
            network.closeServerSocket();
        }
        if (threadPool != null && !threadPool.isShutdown()) {
            threadPool.shutdown();
        }
    }
}
