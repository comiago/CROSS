package client;

import util.MessageBuilder;
import com.google.gson.JsonObject;
import java.io.IOException;

public class Listener extends Thread {
    private final Network network;
    private final MessageHandler messageHandler;
    private volatile boolean running = true;
    private static final MessageBuilder msgBuilder = new MessageBuilder();

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
                    JsonObject closeMsg = msgBuilder.makeMessage("SERVER", "Connessione chiusa dal server");
                    messageHandler.handleMessage(closeMsg);
                    break;
                }
                messageHandler.handleMessage(response);
            }
        } catch (IOException e) {
            if (running) {
                JsonObject errorMsg = msgBuilder.makeMessage("[ERROR]", "Connessione con il server interrotta: " + e.getMessage());
                messageHandler.handleMessage(errorMsg);
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
