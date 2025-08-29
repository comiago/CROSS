package util;

import com.google.gson.JsonObject;
import model.Client;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * Gestisce le notifiche UDP ai client connessi.
 * Supporta notifiche a singoli, multipli o broadcast.
 */
public class Notifier {

    private final List<Client> clients;

    public Notifier(List<Client> clients) {
        this.clients = clients;
    }

    /**
     * Notifica buyer e seller di una transazione.
     *
     * @param buyerUsername  username del compratore
     * @param sellerUsername username del venditore
     * @param tradeNotification oggetto JSON con informazioni della transazione
     * @throws IOException se si verifica un errore di comunicazione
     */
    public void notifyTrade(String buyerUsername, String sellerUsername, JsonObject tradeNotification) throws IOException {
        boolean buyerNotified = false;
        boolean sellerNotified = false;

        synchronized (clients) {
            Iterator<Client> iterator = clients.iterator();
            while (iterator.hasNext()) {
                Client client = iterator.next();
                try {
                    if (!buyerNotified && client.getUsername().equals(buyerUsername)) {
                        client.notify(tradeNotification);
                        buyerNotified = true;
                    }
                    if (!sellerNotified && client.getUsername().equals(sellerUsername)) {
                        client.notify(tradeNotification);
                        sellerNotified = true;
                    }

                    // Esci se entrambi sono stati notificati
                    if (buyerNotified && sellerNotified) break;

                } catch (IOException e) {
                    removeDisconnectedClient(iterator, client, e);
                }
            }
        }

        logMissingNotifications(buyerUsername, sellerUsername, buyerNotified, sellerNotified);
    }

    /**
     * Notifica tutti i client connessi (broadcast)
     */
    public void notifyAll(JsonObject notification) {
        synchronized (clients) {
            Iterator<Client> iterator = clients.iterator();
            while (iterator.hasNext()) {
                Client client = iterator.next();
                try {
                    client.notify(notification);
                } catch (IOException e) {
                    removeDisconnectedClient(iterator, client, e);
                }
            }
        }
    }

    /**
     * Notifica un client specifico per username.
     *
     * @return true se l'utente è stato notificato correttamente, false altrimenti
     */
    public boolean notifyUser(String username, JsonObject notification) {
        synchronized (clients) {
            Iterator<Client> iterator = clients.iterator();
            while (iterator.hasNext()) {
                Client client = iterator.next();
                if (client.getUsername().equals(username)) {
                    try {
                        client.notify(notification);
                        return true;
                    } catch (IOException e) {
                        removeDisconnectedClient(iterator, client, e);
                        return false;
                    }
                }
            }
        }
        System.out.println("⚠ Client " + username + " non trovato per la notifica");
        return false;
    }

    /**
     * Notifica multipli client per username.
     */
    public void notifyUsers(List<String> usernames, JsonObject notification) {
        synchronized (clients) {
            Iterator<Client> iterator = clients.iterator();
            while (iterator.hasNext()) {
                Client client = iterator.next();
                if (usernames.contains(client.getUsername())) {
                    try {
                        client.notify(notification);
                    } catch (IOException e) {
                        removeDisconnectedClient(iterator, client, e);
                    }
                }
            }
        }
    }

    /**
     * Verifica se un utente è online.
     */
    public boolean isUserOnline(String username) {
        synchronized (clients) {
            return clients.stream().anyMatch(c -> c.getUsername().equals(username));
        }
    }

    /**
     * Restituisce il numero di client connessi.
     */
    public int getConnectedCount() {
        synchronized (clients) {
            return clients.size();
        }
    }

    /* ------------------- METODI PRIVATI ------------------- */

    private void removeDisconnectedClient(Iterator<Client> iterator, Client client, IOException e) {
        System.err.println("Errore notifica a client " + client.getUsername() + ", rimuovo: " + e.getMessage());
        iterator.remove();
    }

    private void logMissingNotifications(String buyerUsername, String sellerUsername,
                                         boolean buyerNotified, boolean sellerNotified) {
        if (!buyerNotified) System.out.println("⚠ Buyer " + buyerUsername + " non connesso, notifica non inviata");
        if (!sellerNotified) System.out.println("⚠ Seller " + sellerUsername + " non connesso, notifica non inviata");
    }
}
