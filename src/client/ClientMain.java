package client;

import model.ClientConfig;
import util.ConfigFileManager;
import util.Colors;

import java.io.*;
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

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            network.close();
            System.out.println("\nApplicazione interrotta.");
        }));

        try {
            config = ConfigFileManager.loadConfig("src/client/client.config", ClientConfig.class);
            System.out.println("Loaded config for client: " + config);

            String address = config.getServerAddress();
            int port = config.getServerPort();
            network = new Network(address, port);
            handler = new CommandHandler();

            listenerThread = new Listener(network, message -> {
                synchronized (consoleLock) {
                    if (message.has("response")) {
                        int response = message.get("response").getAsInt();
                        String errorMessage = message.get("errorMessage").getAsString();
                        if (response == 100) {
                            System.out.print("\r" + Colors.GREEN + "[SERVER] " + errorMessage + Colors.RESET + "\n");
                        } else {
                            System.out.print("\r" + Colors.RED + "[SERVER] " + errorMessage + Colors.RESET + "\n");
                        }
                        // Ripristina la prompt
                        printPrompt();
                    }
                }
            });
            listenerThread.start();

            startUserShell();
        } catch (IllegalAccessException | InstantiationException | IOException e) {
            System.err.println("Errore durante il caricamento della configurazione: " + e.getMessage());
        } finally {
            if (network != null) {
                network.close();
            }
        }
    }

    private static void printPrompt() {
        System.out.print(Colors.BLUE + "$" + user + " > " + Colors.RESET);
        System.out.flush();
    }

    private static void startUserShell() {
        Scanner scanner = new Scanner(System.in);

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
                throw new RuntimeException(e);
            }

        }
        scanner.close();
    }
}
