package client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import util.MessageBuilder;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class UdpListener extends Thread {
    private final DatagramSocket udpSocket;
    private final Listener.MessageHandler handler;
    private volatile boolean running = true;
    private static final int BUFFER_SIZE = 1024;
    private static final MessageBuilder msgBuilder = new MessageBuilder();

    public UdpListener(DatagramSocket udpSocket, Listener.MessageHandler handler) {
        this.udpSocket = udpSocket;
        this.handler = handler;
    }

    @Override
    public void run() {
        byte[] buffer = new byte[BUFFER_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        while (running && !udpSocket.isClosed()) {
            try {
                udpSocket.receive(packet);
                String msg = new String(packet.getData(), 0, packet.getLength());

                try {
                    JsonObject json = JsonParser.parseString(msg).getAsJsonObject();
                    handler.handleMessage(json);
                } catch (Exception e) {
                    handler.handleMessage(msgBuilder.makeMessage("[UDP-ERROR]", "Messaggio UDP malformato: " + msg));
                }

            } catch (IOException e) {
                if (running) {
                    handler.handleMessage(msgBuilder.makeMessage("[UDP-ERROR]", "Errore ricezione UDP: " + e.getMessage()));
                }
            }
        }
    }

    public void stopListening() {
        running = false;
        this.interrupt(); // sblocca udpSocket.receive() solo se necessario
        udpSocket.close(); // forza la chiusura per uscire dal blocco
    }
}
