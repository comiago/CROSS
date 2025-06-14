package model;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.*;
import java.util.concurrent.locks.ReentrantLock;
import util.MessageBuilder;

public class Client {
    private final int id;
    private String username;
    private final Socket tcpSocket;
    private InetAddress udpAddress;
    private int udpPort;
    private DatagramSocket udpSocket;
    private final ReentrantLock lock = new ReentrantLock();
    private MessageBuilder msgBuilder;

    public Client(int id, Socket socket) {
        this.id = id;
        this.username = "user" + id;
        this.tcpSocket = socket;
        msgBuilder = new MessageBuilder();
    }

    public void setUdpPort(int port) {
        this.udpPort = port;
        this.udpAddress = InetAddress.getLoopbackAddress();
    }

    public void notify(JsonObject notify) throws IOException {
        if (udpAddress == null || udpPort == 0) {
            System.err.println("UDP client address or port not set.");
            return;
        }

        byte[] data = notify.toString().getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length, udpAddress, udpPort);

        // Usa un socket per l'invio (può essere condiviso tra client, o creato ogni volta)
        if (udpSocket == null || udpSocket.isClosed()) {
            udpSocket = new DatagramSocket(); // socket solo per invio
        }

        udpSocket.send(packet);
    }

    public int getId() {
        return id;
    }

    public Socket getTcpSocket() {
        return tcpSocket;
    }

    public DatagramSocket getUdpSocket() {
        return udpSocket;
    }

    public Socket getSocket() {
        return tcpSocket;
    }

    public String getUsername() {
        lock.lock();
        try {
            return username;
        } finally {
            lock.unlock();
        }
    }

    public void setUsername(String username) {
        lock.lock();
        try {
            this.username = username;
        } finally {
            lock.unlock();
        }
    }

    public void runSafely(Runnable action) {
        lock.lock();
        try {
            action.run();
        } finally {
            lock.unlock();
        }
    }

    public ReentrantLock getLock() {
        return lock;
    }
}
