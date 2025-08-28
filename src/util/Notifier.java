package util;

import com.google.gson.JsonObject;
import model.Client;

import java.io.IOException;
import java.util.List;
import java.util.Iterator;

public class Notifier {
    private final List<Client> clients;

    public Notifier(List<Client> clients) {
        this.clients = clients;
    }

    /**
     * Notifica sia il buyer che il seller di una transazione
     */
    public void notifyTrade(String buyerUsername, String sellerUsername, JsonObject tradeNotification) throws IOException {
        boolean buyerNotified = false;
        boolean sellerNotified = false;

        synchronized (clients) {
            Iterator<Client> iterator = clients.iterator();
            while (iterator.hasNext()) {
                Client client = iterator.next();
                try {
                    if (client.getUsername().equals(buyerUsername)) {
                        client.notify(tradeNotification);
                        buyerNotified = true;
                    }
                    if (client.getUsername().equals(sellerUsername)) {
                        client.notify(tradeNotification);
                        sellerNotified = true;
                    }

                    // Se entrambi sono stati notificati, possiamo uscire prima
                    if (buyerNotified && sellerNotified) {
                        break;
                    }
                } catch (IOException e) {
                    System.err.println("Errore notifica a client " + client.getUsername() + ", rimuovo: " + e.getMessage());
                    iterator.remove(); // Rimuovi client disconnesso
                }
            }
        }

        // Log per debugging
        if (!buyerNotified) {
            System.out.println("⚠ Buyer " + buyerUsername + " non connesso, notifica non inviata");
        }
        if (!sellerNotified) {
            System.out.println("⚠ Seller " + sellerUsername + " non connesso, notifica non inviata");
        }
    }

    /**
     * Notifica tutti i client connessi
     */
    public void notifyAll(JsonObject notification) {
        synchronized (clients) {
            Iterator<Client> iterator = clients.iterator();
            while (iterator.hasNext()) {
                Client client = iterator.next();
                try {
                    client.notify(notification);
                } catch (IOException e) {
                    System.err.println("Errore notifica broadcast a " + client.getUsername() + ", rimuovo: " + e.getMessage());
                    iterator.remove();
                }
            }
        }
    }

    /**
     * Notifica un client specifico per username
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
                        System.err.println("Errore notifica a " + username + ", rimuovo: " + e.getMessage());
                        iterator.remove();
                        return false;
                    }
                }
            }
        }
        System.out.println("⚠ Client " + username + " non trovato per la notifica");
        return false;
    }

    /**
     * Notifica multipli client per username
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
                        System.err.println("Errore notifica a " + client.getUsername() + ", rimuovo: " + e.getMessage());
                        iterator.remove();
                    }
                }
            }
        }
    }

    /**
     * Verifica se un utente è online
     */
    public boolean isUserOnline(String username) {
        synchronized (clients) {
            for (Client client : clients) {
                if (client.getUsername().equals(username)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Ottiene il numero di client connessi
     */
    public int getConnectedCount() {
        synchronized (clients) {
            return clients.size();
        }
    }
}