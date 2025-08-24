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
        TreeMap<Integer, List<LimitOrder>> bookSide = order.getSide() == Side.bid ? bids : asks;
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
            TreeMap<Integer, List<LimitOrder>> bookSide = limitOrder.getSide() == Side.bid ? bids : asks;
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

    public synchronized List<ExecutedTrade> matchLimitOrder(LimitOrder newOrder) {
        List<ExecutedTrade> trades = new ArrayList<>();

        // 1. Identifica il lato opposto del book
        TreeMap<Integer, List<LimitOrder>> oppositeBook =
                newOrder.getSide() == Side.bid ? asks : bids;

        // 2. Itera sui prezzi ordinati favorevolmente
        Iterator<Map.Entry<Integer, List<LimitOrder>>> it = oppositeBook.entrySet().iterator();

        while (it.hasNext() && newOrder.getSize() > 0) {
            Map.Entry<Integer, List<LimitOrder>> entry = it.next();
            int price = entry.getKey();

            // 3. Verifica compatibilità prezzo (limit condition)
            boolean isMatch = newOrder.getSide() == Side.bid
                    ? newOrder.getPrice() >= price
                    : newOrder.getPrice() <= price;

            if (!isMatch) break; // fermati: prezzi non più compatibili

            List<LimitOrder> ordersAtPrice = entry.getValue();
            Iterator<LimitOrder> orderIt = ordersAtPrice.iterator();

            // 4. Itera sugli ordini allo stesso prezzo
            while (orderIt.hasNext() && newOrder.getSize() > 0) {
                LimitOrder existingOrder = orderIt.next();

                // 5. Calcola quanto posso scambiare
                int matchedSize = Math.min(existingOrder.getSize(), newOrder.getSize());

                // 6. Aggiorna size residue
                existingOrder.setSize(existingOrder.getSize() - matchedSize);
                newOrder.setSize(newOrder.getSize() - matchedSize);

                // 7. Crea ExecutedTrade
                trades.add(createTrade(newOrder, existingOrder, price, matchedSize));

                // 8. Rimuovi l’ordine esistente se esaurito
                if (existingOrder.getSize() == 0) {
                    orderIt.remove();
                    allOrders.remove(existingOrder.getOrderId());
                }
            }

            // 9. Se la lista di quel prezzo è vuota, rimuovila dal book
            if (ordersAtPrice.isEmpty()) {
                it.remove();
            }
        }

        // 10. Se l’ordine è ancora attivo, va aggiunto nel suo lato del book
        if (newOrder.getSize() > 0) {
            addLimitOrder(newOrder);
        } else {
            allOrders.remove(newOrder.getOrderId());
        }

        return trades;
    }

    private ExecutedTrade createTrade(Order newOrder, Order existingOrder, int price, int size) {
        Client buyer = newOrder.getSide() == Side.bid ? newOrder.getUser() : existingOrder.getUser();
        Client seller = newOrder.getSide() == Side.ask ? newOrder.getUser() : existingOrder.getUser();

        return new ExecutedTrade(
                buyer,
                seller,
                newOrder.getSide(),
                size,
                price,
                System.currentTimeMillis()
        );
    }

}
