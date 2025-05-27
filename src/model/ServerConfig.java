package model;

public class ServerConfig {
    private String serverAddress;
    private int serverPort;

    private int backlog;
    private int maxClients;

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
        return "Config{" +
                "host='" + serverAddress + '\'' +
                ", port=" + serverPort +
                ", backlog=" + backlog +
                ", maxClients=" + maxClients +
                '}';
    }
}
