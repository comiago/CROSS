package client;

import util.MessageBuilder;
import com.google.gson.JsonObject;
import java.io.IOException;

public class Listener extends Thread {
    private final Network network;
    private final MessageHandler messageHandler;
    private volatile boolean running = true;
    private static MessageBuilder msgBuilder = new MessageBuilder();

    public interface MessageHandler {
        void handleMessage(JsonObject message);
    }

    public Listener(Network network, MessageHandler handler) {
        this.network = network;
        this.messageHandler = handler;
    }

    @Override
    public void run() {
        try {
            while (running && network.isConnected()) {
                JsonObject response = network.readJsonResponse();
                if (response == null) {
                    // Connessione chiusa dal server
                    messageHandler.handleMessage(msgBuilder.makeMessage("SERVER", "Connessione chiusa dal server"));
                    break;
                }
                messageHandler.handleMessage(response);
            }
        } catch (IOException e) {
            if (running) {
                messageHandler.handleMessage(msgBuilder.makeMessage("[ERROR]", "Connessione con il server interrotta: " + e.getMessage()));
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
