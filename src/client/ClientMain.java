package client;

import com.google.gson.JsonObject;
import model.ClientConfig;
import util.Colors;
import util.ConfigFileManager;

import java.io.IOException;
import java.util.Scanner;

/**
 * Classe principale del client CROSS.
 * Si occupa di:
 *  - caricare la configurazione del client
 *  - gestire la connessione TCP/UDP con il server
 *  - avviare la shell interattiva per l'utente
 */
public class ClientMain {

    private static ClientConfig config;            // Configurazione del client (host, port)
    private static String user = "user";           // Nome utente visualizzato nel prompt
    private static Network network;                // Gestione socket TCP/UDP
    private static Listener listenerThread;        // Thread TCP listener
    private static UdpListener udpListener;        // Thread UDP listener
    private static final Object consoleLock = new Object(); // Lock per sincronizzare output su console
    private static volatile boolean running = true;          // Flag per terminare il client
    private static CommandHandler handler;         // Parser comandi da shell

    public static void main(String[] args) {
        // Shutdown hook per chiusura pulita del client
        Runtime.getRuntime().addShutdownHook(new Thread(ClientMain::shutdown));

        try {
            // Caricamento configurazione client
            config = ConfigFileManager.loadConfig("src/config/client.config", ClientConfig.class);
            System.out.println("Loaded config for client: " + config);

            // Creazione network TCP/UDP
            network = new Network(config.getServerAddress(), config.getServerPort());
            handler = new CommandHandler();

            // Avvio listener TCP per messaggi server
            listenerThread = new Listener(network, ClientMain::handleMessage);
            listenerThread.start();

            // Avvio listener UDP per notifiche (trade, ecc.)
            udpListener = new UdpListener(network.getUdpSocket(), ClientMain::handleMessage);
            udpListener.start();

            // Avvio shell interattiva
            startUserShell();

        } catch (IOException | ReflectiveOperationException e) {
            System.err.println("Errore durante l'avvio del client: " + e.getMessage());
        }
    }

    /**
     * Metodo per chiusura sicura del client
     */
    private static void shutdown() {
        running = false;

        if (network != null) network.close();
        if (listenerThread != null) listenerThread.stopListening();
        if (udpListener != null) udpListener.stopListening();

        System.out.println("\nApplicazione interrotta.");
    }

    /**
     * Shell interattiva per input dell'utente
     */
    private static void startUserShell() {
        try (Scanner scanner = new Scanner(System.in)) {
            while (running) {
                printPrompt();              // Mostra prompt
                String input = scanner.nextLine();
                handleUserInput(input);     // Gestisce input
            }
        }
    }

    /**
     * Gestisce input utente: parsing e invio al server
     */
    private static void handleUserInput(String input) {
        try {
            JsonObject request = handler.parseCommand(input);
            network.sendJsonRequest(request);

        } catch (IllegalArgumentException e) {
            printToConsole("[ERROR] " + e.getMessage(), Colors.RED);

        } catch (IOException e) {
            printToConsole("[ERROR] Impossibile inviare richiesta: " + e.getMessage(), Colors.RED);
        }
    }

    /**
     * Gestisce messaggi ricevuti da TCP o UDP
     */
    private static void handleMessage(JsonObject message) {
        synchronized (consoleLock) {
            if (message.has("response")) {  // Risposte standard del server
                int response = message.get("response").getAsInt();
                String errorMessage = message.get("errorMessage").getAsString();
                printToConsole("[SERVER] " + errorMessage, response == 100 ? Colors.GREEN : Colors.RED);

            } else if (message.has("orderId")) { // Risposta a ordini inseriti
                int orderId = message.get("orderId").getAsInt();
                if (orderId > 0) {
                    printToConsole("[SERVER] Order submitted - id: " + orderId, Colors.GREEN);
                } else {
                    printToConsole("[SERVER] Qualcosa è andato storto", Colors.RED);
                }

            } else if (message.has("notification")) { // Notifiche trade
                String notification = message.get("notification").getAsString();
                printToConsole("[NOTIFICATION] " + notification + " " + message.get("trades"), Colors.YELLOW);
            }

            printPrompt();
        }
    }

    /**
     * Stampa prompt utente
     */
    private static void printPrompt() {
        synchronized (consoleLock) {
            System.out.print(Colors.BLUE + "$" + user + " > " + Colors.RESET);
            System.out.flush();
        }
    }

    /**
     * Utility per stampare in console con colore
     */
    private static void printToConsole(String msg, String color) {
        synchronized (consoleLock) {
            System.out.print("\r" + color + msg + Colors.RESET + "\n");
        }
    }
}
