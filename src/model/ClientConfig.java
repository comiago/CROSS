package model;

public class ClientConfig {
    private String serverAddress;
    private int serverPort;

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
        return "Config{" +
                "host='" + serverAddress + '\'' +
                ", port=" + serverPort +
                '}';
    }
}
