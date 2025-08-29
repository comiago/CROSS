package client;

import util.MessageBuilder;
import com.google.gson.JsonObject;
import java.io.IOException;

/**
 * Thread che si occupa di ascoltare i messaggi provenienti dal server TCP.
 * Riceve JSON e li passa al MessageHandler fornito.
 */
public class Listener extends Thread {

    private final Network network;                  // Oggetto per leggere/scrivere sul socket TCP
    private final MessageHandler messageHandler;    // Callback per gestire i messaggi ricevuti
    private volatile boolean running = true;        // Flag per fermare il thread in sicurezza
    private static final MessageBuilder msgBuilder = new MessageBuilder(); // Utility per costruire messaggi JSON

    /**
     * Interfaccia per la gestione dei messaggi ricevuti.
     */
    public interface MessageHandler {
        void handleMessage(JsonObject message);
    }

    /**
     * Costruttore del Listener TCP
     * @param network Oggetto Network collegato al server
     * @param handler Callback per gestire i messaggi
     */
    public Listener(Network network, MessageHandler handler) {
        this.network = network;
        this.messageHandler = handler;
    }

    /**
     * Ciclo principale del thread che legge continuamente dal socket TCP.
     */
    @Override
    public void run() {
        while (running && network.isConnected()) {
            try {
                // Legge un JSON dal server
                JsonObject response = network.readJsonResponse();

                if (response == null) {
                    // Connessione chiusa dal server
                    sendCloseMessage();
                    break;
                }

                // Passa il JSON al gestore dei messaggi
                messageHandler.handleMessage(response);

            } catch (IOException e) {
                // Gestione errori di I/O
                if (running) {
                    sendErrorMessage("Connessione con il server interrotta: " + e.getMessage());
                }
                break;
            }
        }
        running = false; // Assicura che il thread sia fermo
    }

    /**
     * Ferma il listener in modo sicuro.
     */
    public void stopListening() {
        running = false;
        this.interrupt(); // Sblocca eventuali read bloccanti sul socket
    }

    /**
     * Costruisce e invia un messaggio JSON di chiusura connessione
     */
    private void sendCloseMessage() {
        JsonObject closeMsg = msgBuilder.makeMessage("SERVER", "Connessione chiusa dal server");
        messageHandler.handleMessage(closeMsg);
    }

    /**
     * Costruisce e invia un messaggio JSON di errore
     */
    private void sendErrorMessage(String msg) {
        JsonObject errorMsg = msgBuilder.makeMessage("[ERROR]", msg);
        messageHandler.handleMessage(errorMsg);
    }
}
