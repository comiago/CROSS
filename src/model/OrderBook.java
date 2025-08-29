package model;

import util.OrderStorage;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OrderBook gestisce tutti gli ordini (Limit, Market, Stop) e le loro esecuzioni.
 * Mantiene:
 * - Limit order book (bids e asks)
 * - Stop order book (bids e asks)
 * - Mappa globale di tutti gli ordini indicizzati per ID
 */
public class OrderBook {

    // TreeMap per BID limit order (miglior prezzo in cima → decrescente)
    private final TreeMap<Integer, List<LimitOrder>> limitBids;

    // TreeMap per ASK limit order (miglior prezzo in cima → crescente)
    private final TreeMap<Integer, List<LimitOrder>> limitAsks;

    // TreeMap per STOP BIDs (trigger quando prezzo >= stopPrice)
    private final TreeMap<Integer, List<StopOrder>> stopBids;

    // TreeMap per STOP ASKs (trigger quando prezzo <= stopPrice)
    private final TreeMap<Integer, List<StopOrder>> stopAsks;

    // Tutti gli ordini indicizzati per orderId
    private final Map<Long, Order> allOrders;

    // Ultimo orderId generato
    private long lastOrderId;

    public OrderBook() {
        // Inizializzazione delle strutture dati
        this.limitBids = new TreeMap<>(Collections.reverseOrder());
        this.limitAsks = new TreeMap<>();
        this.stopBids = new TreeMap<>();
        this.stopAsks = new TreeMap<>(Collections.reverseOrder());
        this.allOrders = new ConcurrentHashMap<>();
        this.lastOrderId = 0;

        // Caricamento ordini pending da file
        try {
            Map<String, List<LimitOrder>> pendingLimits = OrderStorage.loadPendingLimitOrders();
            loadLimitFromStorage(pendingLimits);
            System.out.println("✅ Limit orders caricati da file: "
                    + pendingLimits.getOrDefault("bids", List.of()).size() + " bids, "
                    + pendingLimits.getOrDefault("asks", List.of()).size() + " asks");
        } catch (Exception e) {
            System.err.println("❌ Errore caricamento limit orders: " + e.getMessage());
        }

        try {
            Map<String, List<StopOrder>> pendingStops = OrderStorage.loadPendingStopOrders();
            loadStopFromStorage(pendingStops);
            System.out.println("✅ Stop orders caricati da file: "
                    + pendingStops.getOrDefault("bids", List.of()).size() + " bids, "
                    + pendingStops.getOrDefault("asks", List.of()).size() + " asks");
        } catch (Exception e) {
            System.err.println("❌ Errore caricamento stop orders: " + e.getMessage());
        }
    }

    // --- Generazione ID univoco per ogni ordine ---
    public synchronized long generateOrderId() {
        return ++lastOrderId;
    }

    // --- Aggiunta ordini limit o stop al book ---
    public synchronized void addLimitOrder(LimitOrder order) {
        TreeMap<Integer, List<LimitOrder>> bookSide = order.getSide() == Side.BID ? limitBids : limitAsks;
        bookSide.computeIfAbsent(order.getPrice(), k -> new LinkedList<>()).add(order);
        allOrders.put(order.getOrderId(), order);
    }

    public synchronized void addStopOrder(StopOrder order) {
        TreeMap<Integer, List<StopOrder>> bookSide = order.getSide() == Side.BID ? stopBids : stopAsks;
        bookSide.computeIfAbsent(order.getStopPrice(), k -> new LinkedList<>()).add(order);
        allOrders.put(order.getOrderId(), order);
    }

    // --- Rimuove un ordine dato l'ID ---
    public synchronized boolean removeOrder(long orderId) {
        Order order = allOrders.remove(orderId);
        if (order == null) return false;

        if (order instanceof LimitOrder limitOrder) {
            TreeMap<Integer, List<LimitOrder>> bookSide = limitOrder.getSide() == Side.BID ? limitBids : limitAsks;
            List<LimitOrder> ordersAtPrice = bookSide.get(limitOrder.getPrice());
            if (ordersAtPrice != null) {
                ordersAtPrice.remove(limitOrder);
                if (ordersAtPrice.isEmpty()) {
                    bookSide.remove(limitOrder.getPrice());
                }
            }
        }
        // Market e Stop orders richiedono logiche diverse
        return true;
    }

    // --- Accesso agli ordini ---
    public Order getOrder(long orderId) {
        return allOrders.get(orderId);
    }

    public Map<Integer, List<LimitOrder>> getLimitBids() { return Collections.unmodifiableMap(limitBids); }
    public Map<Integer, List<LimitOrder>> getLimitAsks() { return Collections.unmodifiableMap(limitAsks); }
    public Map<Integer, List<StopOrder>> getStopBids() { return Collections.unmodifiableMap(stopBids); }
    public Map<Integer, List<StopOrder>> getStopAsks() { return Collections.unmodifiableMap(stopAsks); }

    // --- Caricamento ordini da storage ---
    private void loadLimitFromStorage(Map<String, List<LimitOrder>> pending) {
        pending.getOrDefault("bids", List.of()).forEach(bid -> {
            limitBids.computeIfAbsent(bid.getPrice(), k -> new LinkedList<>()).add(bid);
            allOrders.put(bid.getOrderId(), bid);
        });
        pending.getOrDefault("asks", List.of()).forEach(ask -> {
            limitAsks.computeIfAbsent(ask.getPrice(), k -> new LinkedList<>()).add(ask);
            allOrders.put(ask.getOrderId(), ask);
        });
    }

    private void loadStopFromStorage(Map<String, List<StopOrder>> pending) {
        pending.getOrDefault("bids", List.of()).forEach(bid -> {
            stopBids.computeIfAbsent(bid.getStopPrice(), k -> new LinkedList<>()).add(bid);
            allOrders.put(bid.getOrderId(), bid);
        });
        pending.getOrDefault("asks", List.of()).forEach(ask -> {
            stopAsks.computeIfAbsent(ask.getStopPrice(), k -> new LinkedList<>()).add(ask);
            allOrders.put(ask.getOrderId(), ask);
        });
    }

    // --- Matching di ordini limit ---
    public synchronized List<ExecutedTrade> matchLimitOrder(LimitOrder newOrder) {
        List<ExecutedTrade> trades = new ArrayList<>();
        TreeMap<Integer, List<LimitOrder>> oppositeBook = newOrder.getSide() == Side.BID ? limitAsks : limitBids;

        Iterator<Map.Entry<Integer, List<LimitOrder>>> it = oppositeBook.entrySet().iterator();
        while (it.hasNext() && newOrder.getSize() > 0) {
            Map.Entry<Integer, List<LimitOrder>> entry = it.next();
            int price = entry.getKey();

            boolean isMatch = newOrder.getSide() == Side.BID ? newOrder.getPrice() >= price : newOrder.getPrice() <= price;
            if (!isMatch) break;

            List<LimitOrder> ordersAtPrice = entry.getValue();
            Iterator<LimitOrder> orderIt = ordersAtPrice.iterator();
            while (orderIt.hasNext() && newOrder.getSize() > 0) {
                LimitOrder existingOrder = orderIt.next();
                int matchedSize = Math.min(existingOrder.getSize(), newOrder.getSize());
                existingOrder.setSize(existingOrder.getSize() - matchedSize);
                newOrder.setSize(newOrder.getSize() - matchedSize);

                trades.add(createTrade(newOrder, existingOrder, price, matchedSize));

                if (existingOrder.getSize() == 0) {
                    orderIt.remove();
                    allOrders.remove(existingOrder.getOrderId());
                }
            }

            if (ordersAtPrice.isEmpty()) it.remove();
        }

        if (newOrder.getSize() > 0) addLimitOrder(newOrder);
        else allOrders.remove(newOrder.getOrderId());

        // Trigger stop orders se ci sono trade
        if (!trades.isEmpty()) {
            int lastPrice = trades.get(trades.size() - 1).getPrice();
            List<MarketOrder> triggered = triggerStopOrders(lastPrice);
            for (MarketOrder mo : triggered) trades.addAll(matchMarketOrder(mo));
        }

        OrderStorage.savePendingStopOrders(this);
        return trades;
    }

    // --- Matching di ordini market ---
    public synchronized List<ExecutedTrade> matchMarketOrder(MarketOrder order) {
        List<ExecutedTrade> trades = new ArrayList<>();
        TreeMap<Integer, List<LimitOrder>> oppositeBook = order.getSide() == Side.BID ? limitAsks : limitBids;

        Iterator<Map.Entry<Integer, List<LimitOrder>>> it = oppositeBook.entrySet().iterator();
        while (it.hasNext() && order.getSize() > 0) {
            Map.Entry<Integer, List<LimitOrder>> entry = it.next();
            List<LimitOrder> ordersAtPrice = entry.getValue();
            Iterator<LimitOrder> orderIt = ordersAtPrice.iterator();

            while (orderIt.hasNext() && order.getSize() > 0) {
                LimitOrder existingOrder = orderIt.next();
                int matchedSize = Math.min(existingOrder.getSize(), order.getSize());
                existingOrder.setSize(existingOrder.getSize() - matchedSize);
                order.setSize(order.getSize() - matchedSize);

                trades.add(createTrade(order, existingOrder, entry.getKey(), matchedSize));

                if (existingOrder.getSize() == 0) {
                    orderIt.remove();
                    allOrders.remove(existingOrder.getOrderId());
                }
            }

            if (ordersAtPrice.isEmpty()) it.remove();
        }

        if (order.getSize() > 0) {
            System.out.println("⚠ Residuo MarketOrder scartato (non va in pending).");
            allOrders.remove(order.getOrderId());
        }

        if (!trades.isEmpty()) {
            int lastPrice = trades.get(trades.size() - 1).getPrice();
            List<MarketOrder> triggered = triggerStopOrders(lastPrice);
            for (MarketOrder mo : triggered) trades.addAll(matchMarketOrder(mo));
        }

        OrderStorage.savePendingStopOrders(this);
        return trades;
    }

    // --- Trigger ordini stop basati sull'ultimo prezzo ---
    public synchronized List<MarketOrder> triggerStopOrders(int lastTradedPrice) {
        List<MarketOrder> triggered = new ArrayList<>();

        // STOP BIDs → trigger se prezzo >= stopPrice
        Iterator<Map.Entry<Integer, List<StopOrder>>> itBids = stopBids.entrySet().iterator();
        while (itBids.hasNext()) {
            Map.Entry<Integer, List<StopOrder>> entry = itBids.next();
            if (lastTradedPrice >= entry.getKey()) {
                for (StopOrder so : entry.getValue()) {
                    triggered.add(new MarketOrder(so.getOrderId(), so.getUsername(), Side.BID, so.getSize()));
                    allOrders.remove(so.getOrderId());
                }
                itBids.remove();
            } else break;
        }

        // STOP ASKs → trigger se prezzo <= stopPrice
        Iterator<Map.Entry<Integer, List<StopOrder>>> itAsks = stopAsks.entrySet().iterator();
        while (itAsks.hasNext()) {
            Map.Entry<Integer, List<StopOrder>> entry = itAsks.next();
            if (lastTradedPrice <= entry.getKey()) {
                for (StopOrder so : entry.getValue()) {
                    triggered.add(new MarketOrder(so.getOrderId(), so.getUsername(), Side.ASK, so.getSize()));
                    allOrders.remove(so.getOrderId());
                }
                itAsks.remove();
            } else break;
        }

        OrderStorage.savePendingStopOrders(this);
        return triggered;
    }

    // --- Utility per creare un trade eseguito ---
    private ExecutedTrade createTrade(Order newOrder, Order existingOrder, int price, int size) {
        String buyer = newOrder.getSide() == Side.BID ? newOrder.getUsername() : existingOrder.getUsername();
        String seller = newOrder.getSide() == Side.ASK ? newOrder.getUsername() : existingOrder.getUsername();

        return new ExecutedTrade(
                buyer,
                seller,
                newOrder.getOrderType(),
                newOrder.getSide(),
                size,
                price,
                System.currentTimeMillis()
        );
    }
}
