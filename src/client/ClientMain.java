package client;

import model.ClientConfig;
import util.ConfigFileManager;
import util.Colors;
import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class ClientMain {
    private static ClientConfig config = null;
    private static Socket socket = null;
    private static BufferedReader in = null;
    private static PrintWriter out = null;
    private static String user = "user";

    public static void main(String[] args) {
        // Hook per la chiusura ordinata (CTRL+C)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            closeSocket();
            System.out.println("Applicazione interrotta.");
        }));

        try {
            // Caricamento della configurazione
            config = ConfigFileManager.loadConfig("src/client/client.config", ClientConfig.class);
            System.out.println("Loaded config for client: " + config);

            // Inizializzazione del socket
            socket = new Socket(config.getServerAddress(), config.getServerPort());
            System.out.println("Connessione stabilita con " + config.getServerAddress() + ":" + config.getServerPort());

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            startUserShell();
        } catch (IOException e) {
            System.err.println("Errore durante la connessione o I/O: " + e.getMessage());
        } catch (IllegalAccessException | InstantiationException e) {
            System.err.println("Errore durante il caricamento della configurazione: " + e.getMessage());
        } finally {
            closeSocket(); // Mi assicuro che il socket venga chiuso anche senza SIGINT
        }
    }

    private static void startUserShell() {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print(Colors.BLUE + "$" + user + " > ");
            String message = scanner.nextLine();

            if ("exit".equalsIgnoreCase(message)) {
                System.out.println("Chiusura client...");
                break;
            }

            out.println(message);

            try {
                String response = in.readLine();
                if (response != null) {
                    System.out.println("Server: " + response);
                }
            } catch (IOException e) {
                System.err.println("Errore nella lettura della risposta del server: " + e.getMessage());
                break;
            }
        }
        scanner.close();
    }

    private static void closeSocket() {
        if (socket != null && !socket.isClosed()) {
            try {
                System.out.println("Chiusura della connessione...");
                socket.close();
            } catch (IOException e) {
                System.err.println("Errore durante la chiusura del socket: " + e.getMessage());
            }
        }
    }
}
