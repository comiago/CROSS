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
    private final MessageBuilder msgBuilder;

    public Client(int id, Socket socket) {
        this.id = id;
        this.username = "user" + id;
        this.tcpSocket = socket;
        this.msgBuilder = new MessageBuilder();
    }

    public void setUdpPort(int port) {
        this.udpPort = port;
        // Considera se udpAddress può variare: qui è sempre loopback
        this.udpAddress = InetAddress.getLoopbackAddress();
    }

    // Invia una notifica UDP al client in modo thread-safe
    public void notify(JsonObject notify) throws IOException {
        if (udpAddress == null || udpPort == 0) {
            System.err.println("UDP client address or port not set.");
            return;
        }

        byte[] data = notify.toString().getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length, udpAddress, udpPort);

        // Se il socket UDP non è aperto o chiuso, lo (ri)crea
        if (udpSocket == null || udpSocket.isClosed()) {
            udpSocket = new DatagramSocket();
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

    // Esegue un'azione protetta dal lock
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
