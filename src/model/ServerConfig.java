package model;

/**
 * Classe che rappresenta la configurazione del server.
 * Contiene indirizzo, porta, backlog e numero massimo di client.
 */
public class ServerConfig {
    private String serverAddress; // Indirizzo/IP del server
    private int serverPort;       // Porta TCP del server
    private int backlog;          // Numero massimo di connessioni pendenti
    private int maxClients;       // Numero massimo di client simultanei

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

    public int getBacklog() {
        return backlog;
    }

    public void setBacklog(int backlog) {
        this.backlog = backlog;
    }

    public int getMaxClients() {
        return maxClients;
    }

    public void setMaxClients(int maxClients) {
        this.maxClients = maxClients;
    }

    @Override
    public String toString() {
        return "ServerConfig{" +
                "serverAddress='" + serverAddress + '\'' +
                ", serverPort=" + serverPort +
                ", backlog=" + backlog +
                ", maxClients=" + maxClients +
                '}';
    }
}
