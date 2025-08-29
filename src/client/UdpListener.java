package client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import util.MessageBuilder;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * Thread che ascolta i messaggi UDP dal server.
 * I messaggi ricevuti vengono inoltrati al MessageHandler del client.
 */
public class UdpListener extends Thread {

    private final DatagramSocket udpSocket;
    private final Listener.MessageHandler handler;
    private volatile boolean running = true;

    private static final int BUFFER_SIZE = 1024;
    private static final MessageBuilder msgBuilder = new MessageBuilder();

    /**
     * Costruttore
     * @param udpSocket socket UDP già aperto dal client
     * @param handler handler per processare i messaggi ricevuti
     */
    public UdpListener(DatagramSocket udpSocket, Listener.MessageHandler handler) {
        this.udpSocket = udpSocket;
        this.handler = handler;
    }

    /**
     * Loop principale del thread: riceve pacchetti UDP e li converte in JsonObject.
     * Eventuali errori di parsing o I/O vengono segnalati tramite il handler.
     */
    @Override
    public void run() {
        byte[] buffer = new byte[BUFFER_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        while (running && !udpSocket.isClosed()) {
            try {
                udpSocket.receive(packet); // bloccante
                String msg = new String(packet.getData(), 0, packet.getLength());

                try {
                    JsonObject json = JsonParser.parseString(msg).getAsJsonObject();
                    handler.handleMessage(json);
                } catch (Exception e) {
                    // Messaggio UDP non valido JSON
                    handler.handleMessage(msgBuilder.makeMessage(
                            "[UDP-ERROR]",
                            "Messaggio UDP malformato: " + msg
                    ));
                }

            } catch (IOException e) {
                if (running) {
                    // Errore di ricezione, solo se non stiamo stoppando il listener
                    handler.handleMessage(msgBuilder.makeMessage(
                            "[UDP-ERROR]",
                            "Errore ricezione UDP: " + e.getMessage()
                    ));
                }
                // Se running è false, il socket è probabilmente chiuso intenzionalmente
            }
        }
    }

    /**
     * Ferma il listener in sicurezza.
     * Chiude il socket UDP e interrompe il thread se bloccato.
     */
    public void stopListening() {
        running = false;
        this.interrupt(); // sblocca udpSocket.receive()
        if (!udpSocket.isClosed()) {
            udpSocket.close(); // chiude il socket per uscire dal blocco
        }
    }
}
