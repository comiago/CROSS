package client;

import java.io.IOException;

public class Listener extends Thread {
    private final Network network;
    private final MessageHandler messageHandler;
    private volatile boolean running = true;

    public interface MessageHandler {
        void handleMessage(String message);
    }

    public Listener(Network network, MessageHandler handler) {
        this.network = network;
        this.messageHandler = handler;
    }

    @Override
    public void run() {
        try {
            while (running && network.isConnected()) {
                String response = network.readMessage();
                if (response == null) {
                    // Connessione chiusa dal server
                    messageHandler.handleMessage("[SERVER] Connessione chiusa dal server");
                    break;
                }
                messageHandler.handleMessage(response);
            }
        } catch (IOException e) {
            if (running) {
                messageHandler.handleMessage("[ERR] Connessione con il server interrotta: " + e.getMessage());
            }
        } finally {
            stopListening();
        }
    }

    public void stopListening() {
        running = false;
        this.interrupt();
    }
}