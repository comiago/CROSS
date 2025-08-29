package model;

/**
 * Rappresenta la configurazione del client.
 * Contiene l'indirizzo e la porta del server a cui connettersi.
 */
public class ClientConfig {

    /** Indirizzo del server (es. "localhost" o IP) */
    private String serverAddress;

    /** Porta TCP del server */
    private int serverPort;

    // --- Getters e Setters ---

    public String getServerAddress() {
        return serverAddress;
    }

    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    @Override
    public String toString() {
        return "ClientConfig{" +
                "serverAddress='" + serverAddress + '\'' +
                ", serverPort=" + serverPort +
                '}';
    }
}
