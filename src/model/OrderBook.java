package model;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class OrderBook {

    // Bids ordinati per prezzo decrescente (miglior prezzo in cima)
    private final TreeMap<Integer, List<LimitOrder>> bids;

    // Asks ordinati per prezzo crescente (miglior prezzo in cima)
    private final TreeMap<Integer, List<LimitOrder>> asks;

    // Mappa globale di tutti gli ordini (Limit, Market, Stop) indicizzati per ID
    private final Map<Long, Order> allOrders;

    private long lastOrderId;

    public OrderBook() {
        this.bids = new TreeMap<>(Collections.reverseOrder());
        this.asks = new TreeMap<>();
        this.allOrders = new ConcurrentHashMap<>();
        this.lastOrderId = 0;
    }

    /**
     * Genera un ID univoco per ogni nuovo ordine.
     */
    public synchronized long generateOrderId() {
        return ++lastOrderId;
    }

    /**
     * Aggiunge un ordine limit al book.
     */
    public synchronized void addLimitOrder(LimitOrder order) {
        TreeMap<Integer, List<LimitOrder>> bookSide = order.getSide() == Side.BID ? bids : asks;
        bookSide.computeIfAbsent(order.getPrice(), k -> new LinkedList<>()).add(order);
        allOrders.put(order.getOrderId(), order);
    }

    /**
     * Rimuove un ordine dato l'ID. Ritorna true se rimosso, false se non trovato.
     */
    public synchronized boolean removeOrder(long orderId) {
        Order order = allOrders.remove(orderId);
        if (order == null) return false;

        if (order instanceof LimitOrder limitOrder) {
            TreeMap<Integer, List<LimitOrder>> bookSide = limitOrder.getSide() == Side.BID ? bids : asks;
            List<LimitOrder> ordersAtPrice = bookSide.get(limitOrder.getPrice());

            if (ordersAtPrice != null) {
                ordersAtPrice.remove(limitOrder);
                if (ordersAtPrice.isEmpty()) {
                    bookSide.remove(limitOrder.getPrice());
                }
            }
        }

        // Market e Stop orders potrebbero richiedere logiche diverse
        return true;
    }

    /**
     * Restituisce l'ordine associato all'ID, o null se non esiste.
     */
    public Order getOrder(long orderId) {
        return allOrders.get(orderId);
    }

    /**
     * Vista immutabile dei BIDs (acquisti).
     */
    public Map<Integer, List<LimitOrder>> getBids() {
        return Collections.unmodifiableMap(bids);
    }

    /**
     * Vista immutabile degli ASKs (vendite).
     */
    public Map<Integer, List<LimitOrder>> getAsks() {
        return Collections.unmodifiableMap(asks);
    }


}
