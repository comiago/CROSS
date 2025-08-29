package model;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.*;
import java.util.concurrent.locks.ReentrantLock;
import util.MessageBuilder;

/**
 * Rappresenta un client connesso al server.
 * Gestisce TCP/UDP, username e sincronizzazione tramite lock.
 */
public class Client {

    private final int id;                 // ID univoco del client
    private String username;              // Username associato
    private final Socket tcpSocket;       // Socket TCP per comunicazione principale
    private InetAddress udpAddress;       // Indirizzo UDP del client
    private int udpPort;                  // Porta UDP del client
    private DatagramSocket udpSocket;     // Socket UDP lato server
    private final ReentrantLock lock;     // Lock per accesso thread-safe
    private final MessageBuilder msgBuilder;

    /**
     * Costruttore del client.
     * @param id ID univoco
     * @param socket Socket TCP associato
     */
    public Client(int id, Socket socket) {
        this.id = id;
        this.username = "user" + id;
        this.tcpSocket = socket;
        this.lock = new ReentrantLock();
        this.msgBuilder = new MessageBuilder();
    }

    /**
     * Imposta la porta UDP del client.
     * @param port porta UDP
     */
    public void setUdpPort(int port) {
        this.udpPort = port;
        this.udpAddress = InetAddress.getLoopbackAddress(); // Considera se vuoi cambiare indirizzo
    }

    /**
     * Invia una notifica UDP al client in modo thread-safe.
     * @param notify messaggio JSON da inviare
     */
    public void notify(JsonObject notify) throws IOException {
        if (udpAddress == null || udpPort == 0) {
            System.err.println("UDP client address or port not set.");
            return;
        }

        byte[] data = notify.toString().getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length, udpAddress, udpPort);

        // Crea il socket UDP se non esiste o è chiuso
        if (udpSocket == null || udpSocket.isClosed()) {
            udpSocket = new DatagramSocket();
        }

        udpSocket.send(packet);
    }

    // --- Getters e setters thread-safe ---

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

    /**
     * Esegue un'azione protetta dal lock.
     * @param action Runnable da eseguire
     */
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
