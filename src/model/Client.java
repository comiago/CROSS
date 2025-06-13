package model;

import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.locks.ReentrantLock;

public class Client {
    private final int id;
    private String username;
    private final Socket tcpSocket;
    private DatagramSocket udpSocket;
    private final ReentrantLock lock = new ReentrantLock();

    public Client(int id, Socket socket) {
        this.id = id;
        this.username = "user" + id;
        this.tcpSocket = socket;
    }

    public void udpConnect(int port) throws SocketException {
        udpSocket = new DatagramSocket(port);
        System.out.println("UDP connection established on port " + port + " from " + tcpSocket.getInetAddress().getHostAddress());
    }

    public void notify(JsonObject notify) throws IOException {
        byte[] data = notify.toString().getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length, udpSocket.getInetAddress(), udpSocket.getPort());
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
