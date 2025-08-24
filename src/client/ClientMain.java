package client;

import model.ClientConfig;
import util.ConfigFileManager;
import util.Colors;

import java.io.IOException;
import java.util.Scanner;

import com.google.gson.JsonObject;

public class ClientMain {
    private static ClientConfig config = null;
    private static String user = "user";
    private static Network network;
    private static Listener listenerThread;
    private static final Object consoleLock = new Object();
    private static volatile boolean running = true;
    private static CommandHandler handler;
    private static UdpListener udpListener;

    private static void handleMessage(JsonObject message) {
        synchronized (consoleLock) {
            if (message.has("response")) {
                int response = message.get("response").getAsInt();
                String errorMessage = message.get("errorMessage").getAsString();
                if (response == 100) {
                    System.out.print("\r" + Colors.GREEN + "[SERVER] " + errorMessage + Colors.RESET + "\n");
                } else {
                    System.out.print("\r" + Colors.RED + "[SERVER] " + errorMessage + Colors.RESET + "\n");
                }
                printPrompt();
            } else if(message.has("notification")){
                String notification = message.get("notification").getAsString();
                System.out.print("\r" + Colors.YELLOW + "[NOTIFICATION] " + notification + message.get("trades") + Colors.RESET + "\n");
            }
        }
    }

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (network != null) network.close();
            System.out.println("\nApplicazione interrotta.");
        }));

        try {
            config = ConfigFileManager.loadConfig("src/config/client.config", ClientConfig.class);
            System.out.println("Loaded config for client: " + config);

            network = new Network(config.getServerAddress(), config.getServerPort());
            handler = new CommandHandler();

            listenerThread = new Listener(network, ClientMain::handleMessage);
            listenerThread.start();

            udpListener = new UdpListener(network.getUdpSocket(), ClientMain::handleMessage);
            udpListener.start();

            startUserShell();

        } catch (IOException | ReflectiveOperationException e) {
            System.err.println("Errore durante il caricamento della configurazione: " + e.getMessage());
        }
    }

    private static void printPrompt() {
        System.out.print(Colors.BLUE + "$" + user + " > " + Colors.RESET);
        System.out.flush();
    }

    private static void startUserShell() {
        try (Scanner scanner = new Scanner(System.in)) {
            while (running) {
                synchronized (consoleLock) {
                    printPrompt();
                }
                String input = scanner.nextLine();
                try {
                    JsonObject request = handler.parseCommand(input);
                    network.sendJsonRequest(request);
                } catch (IllegalArgumentException e) {
                    synchronized (consoleLock) {
                        System.out.print("\r" + Colors.RED + "[ERROR] " + e.getMessage() + Colors.RESET + "\n");
                    }
                } catch (IOException e) {
                    System.err.println("Errore invio richiesta: " + e.getMessage());
                }
            }
        }
    }
}
