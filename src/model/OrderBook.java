package model;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class OrderBook {
    // Bids ordinati per prezzo decrescente (miglior prezzo in cima)
    private final TreeMap<Integer, List<LimitOrder>> bids;
    // Asks ordinati per prezzo crescente (miglior prezzo in cima)
    private final TreeMap<Integer, List<LimitOrder>> asks;
    // Mappa globale di tutti gli ordini per ID (limit, market, etc.)
    private final Map<Long, Order> allOrders;
    private long lastOrderId;

    public OrderBook() {
        this.bids = new TreeMap<>(Collections.reverseOrder());
        this.asks = new TreeMap<>();
        this.allOrders = new ConcurrentHashMap<>();
        this.lastOrderId = 0;
    }

    /**
     * Genera un nuovo ID ordine unico.
     */
    public synchronized long generateOrderId() {
        return ++lastOrderId;
    }

    /**
     * Aggiunge un limit order al book.
     */
    public synchronized void addLimitOrder(LimitOrder order) {
        TreeMap<Integer, List<LimitOrder>> bookSide = order.getSide() == Side.BID ? bids : asks;
        bookSide.computeIfAbsent(order.getPrice(), k -> new LinkedList<>()).add(order);
        allOrders.put(order.getOrderId(), order);
    }

    /**
     * Rimuove un ordine per ID. Ritorna true se rimosso con successo, false se non trovato.
     */
    public synchronized boolean removeOrder(long orderId) {
        Order order = allOrders.remove(orderId);
        if (order == null) return false;

        if (order instanceof LimitOrder) {
            LimitOrder limitOrder = (LimitOrder) order;
            TreeMap<Integer, List<LimitOrder>> bookSide = limitOrder.getSide() == Side.BID ? bids : asks;
            List<LimitOrder> ordersAtPrice = bookSide.get(limitOrder.getPrice());
            if (ordersAtPrice != null) {
                ordersAtPrice.remove(limitOrder);
                if (ordersAtPrice.isEmpty()) {
                    bookSide.remove(limitOrder.getPrice());
                }
            }
        }
        // Per ordini di tipo Market o Stop potresti avere altra gestione
        return true;
    }

    /**
     * Recupera un ordine dato l'ID.
     */
    public Order getOrder(long orderId) {
        return allOrders.get(orderId);
    }

    /**
     * Ritorna una vista immutabile del book bids.
     */
    public Map<Integer, List<LimitOrder>> getBids() {
        return Collections.unmodifiableMap(bids);
    }

    /**
     * Ritorna una vista immutabile del book asks.
     */
    public Map<Integer, List<LimitOrder>> getAsks() {
        return Collections.unmodifiableMap(asks);
    }

    // Potresti aggiungere metodi per match degli ordini, aggiornamenti, etc.
}
