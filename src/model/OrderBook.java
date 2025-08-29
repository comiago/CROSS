package model;

import util.OrderStorage;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class OrderBook {

    // Bids limit ordinati per prezzo decrescente (miglior prezzo in cima)
    private final TreeMap<Integer, List<LimitOrder>> limitBids;

    // Asks limit ordinati per prezzo crescente (miglior prezzo in cima)
    private final TreeMap<Integer, List<LimitOrder>> limitAsks;

    // Bids stop ordinati per prezzo decrescente (miglior prezzo in cima)
    private final TreeMap<Integer, List<StopOrder>> stopBids;

    // Asks stop ordinati per prezzo crescente (miglior prezzo in cima)
    private final TreeMap<Integer, List<StopOrder>> stopAsks;

    // Mappa globale di tutti gli ordini (Limit, Market, Stop) indicizzati per ID
    private final Map<Long, Order> allOrders;

    private long lastOrderId;

    public OrderBook() {
        this.limitBids = new TreeMap<>(Collections.reverseOrder());
        this.limitAsks = new TreeMap<>();

        // Bids stop ordinati per prezzo crescente (trigger quando prezzo >= stopPrice)
        this.stopBids = new TreeMap<>();

        // Asks stop ordinati per prezzo decrescente (trigger quando prezzo <= stopPrice)
        this.stopAsks = new TreeMap<>(Collections.reverseOrder());

        this.allOrders = new ConcurrentHashMap<>();
        this.lastOrderId = 0;

        try {
            // Carica i pending limit dal file
            Map<String, List<LimitOrder>> pendingLimits = OrderStorage.loadPendingLimitOrders();
            loadLimitFromStorage(pendingLimits);

            System.out.println("✅ OrderBook inizializzato da file: "
                    + pendingLimits.getOrDefault("bids", List.of()).size() + " limit bids, "
                    + pendingLimits.getOrDefault("asks", List.of()).size() + " limit asks, ");
        } catch (Exception e) {
            System.err.println("❌ Errore durante il caricamento degli limit orders: " + e.getMessage());
            System.out.println("✅ OrderBook inizializzato con dati vuoti");
        }

        try {
            // Carica i pending stop dal file
            Map<String, List<StopOrder>> pendingStops = OrderStorage.loadPendingStopOrders();
            loadStopFromStorage(pendingStops);

            System.out.println("✅ OrderBook inizializzato da file: "
                    + pendingStops.getOrDefault("bids", List.of()).size() + " stop bids, "
                    + pendingStops.getOrDefault("asks", List.of()).size() + " stop asks");
        } catch (Exception e) {
            System.err.println("❌ Errore durante il caricamento degli stop orders: " + e.getMessage());
            System.out.println("✅ OrderBook inizializzato con dati vuoti");
        }
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
        TreeMap<Integer, List<LimitOrder>> bookSide = order.getSide() == Side.bid ? limitBids : limitAsks;
        bookSide.computeIfAbsent(order.getPrice(), k -> new LinkedList<>()).add(order);
        allOrders.put(order.getOrderId(), order);
    }

    public synchronized void addStopOrder(StopOrder order) {
        TreeMap<Integer, List<StopOrder>> bookSide = order.getSide() == Side.bid ? stopBids : stopAsks;
        bookSide.computeIfAbsent(order.getStopPrice(), k -> new LinkedList<>()).add(order);
        allOrders.put(order.getOrderId(), order);
    }


    /**
     * Rimuove un ordine dato l'ID. Ritorna true se rimosso, false se non trovato.
     */
    public synchronized boolean removeOrder(long orderId) {
        Order order = allOrders.remove(orderId);
        if (order == null) return false;

        if (order instanceof LimitOrder limitOrder) {
            TreeMap<Integer, List<LimitOrder>> bookSide = limitOrder.getSide() == Side.bid ? limitBids : limitAsks;
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
    public Map<Integer, List<LimitOrder>> getLimitBids() { return Collections.unmodifiableMap(limitBids); }

    /**
     * Vista immutabile degli ASKs (vendite).
     */
    public Map<Integer, List<LimitOrder>> getLimitAsks() { return Collections.unmodifiableMap(limitAsks); }


    public Map<Integer, List<StopOrder>> getStopBids() { return Collections.unmodifiableMap(stopBids); }

    public Map<Integer, List<StopOrder>> getStopAsks() { return Collections.unmodifiableMap(stopAsks); }

    private void loadLimitFromStorage(Map<String, List<LimitOrder>> pending) {
        for (LimitOrder bid : pending.getOrDefault("bids", new ArrayList<>())) {
            limitBids.computeIfAbsent(bid.getPrice(), k -> new LinkedList<>()).add(bid);
            allOrders.put(bid.getOrderId(), bid);
        }
        for (LimitOrder ask : pending.getOrDefault("asks", new ArrayList<>())) {
            limitAsks.computeIfAbsent(ask.getPrice(), k -> new LinkedList<>()).add(ask);
            allOrders.put(ask.getOrderId(), ask);
        }
    }

    private void loadStopFromStorage(Map<String, List<StopOrder>> pending) {
        for (StopOrder bid : pending.getOrDefault("bids", new ArrayList<>())) {
            stopBids.computeIfAbsent(bid.getStopPrice(), k -> new LinkedList<>()).add(bid);
            allOrders.put(bid.getOrderId(), bid);
        }
        for (StopOrder ask : pending.getOrDefault("asks", new ArrayList<>())) {
            stopAsks.computeIfAbsent(ask.getStopPrice(), k -> new LinkedList<>()).add(ask);
            allOrders.put(ask.getOrderId(), ask);
        }
    }

    public synchronized List<ExecutedTrade> matchLimitOrder(LimitOrder newOrder) {
        List<ExecutedTrade> trades = new ArrayList<>();

        // 1. Identifica il lato opposto del book
        TreeMap<Integer, List<LimitOrder>> oppositeBook =
                newOrder.getSide() == Side.bid ? limitAsks : limitBids;

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

        if (!trades.isEmpty()) {
            int lastPrice = trades.get(trades.size() - 1).getPrice();
            List<MarketOrder> triggered = triggerStopOrders(lastPrice);
            for (MarketOrder mo : triggered) {
                trades.addAll(matchMarketOrder(mo));
            }
        }

        OrderStorage.savePendingStopOrders(this);

        return trades;
    }

    public synchronized List<ExecutedTrade> matchMarketOrder(MarketOrder order) {
        List<ExecutedTrade> trades = new ArrayList<>();

        // Opposto: BID → matcha con asks ; ASK → matcha con bids
        TreeMap<Integer, List<LimitOrder>> oppositeBook =
                order.getSide() == Side.bid ? limitAsks : limitBids;

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

            if (ordersAtPrice.isEmpty()) {
                it.remove();
            }
        }

        // Residuo scartato
        if (order.getSize() > 0) {
            System.out.println("⚠ Residuo di MarketOrder scartato (non va in pending).");
            allOrders.remove(order.getOrderId());
        }

        if (!trades.isEmpty()) {
            int lastPrice = trades.get(trades.size() - 1).getPrice();
            List<MarketOrder> triggered = triggerStopOrders(lastPrice);
            for (MarketOrder mo : triggered) {
                trades.addAll(matchMarketOrder(mo));
            }
        }

        OrderStorage.savePendingStopOrders(this);

        return trades;
    }

    public synchronized List<MarketOrder> triggerStopOrders(int lastTradedPrice) {
        System.out.println("\n\nTRIGGERED\n\n");

        List<MarketOrder> triggered = new ArrayList<>();

        // STOP BIDS → attivano se il prezzo di mercato >= stopPrice
        Iterator<Map.Entry<Integer, List<StopOrder>>> itBids = stopBids.entrySet().iterator();
        while (itBids.hasNext()) {
            Map.Entry<Integer, List<StopOrder>> entry = itBids.next();
            if (lastTradedPrice >= entry.getKey()) {
                for (StopOrder so : entry.getValue()) {
                    triggered.add(new MarketOrder(
                            so.getOrderId(),
                            so.getUsername(),
                            Side.bid,
                            so.getSize()
                    ));
                    allOrders.remove(so.getOrderId());
                }
                itBids.remove(); // rimuove la lista di quel prezzo
            } else break;
        }

        // STOP ASKS → attivano se il prezzo di mercato <= stopPrice
        Iterator<Map.Entry<Integer, List<StopOrder>>> itAsks = stopAsks.entrySet().iterator();
        while (itAsks.hasNext()) {
            Map.Entry<Integer, List<StopOrder>> entry = itAsks.next();
            if (lastTradedPrice <= entry.getKey()) {
                for (StopOrder so : entry.getValue()) {
                    triggered.add(new MarketOrder(
                            so.getOrderId(),
                            so.getUsername(),
                            Side.ask,
                            so.getSize()
                    ));
                    allOrders.remove(so.getOrderId());
                }
                itAsks.remove();
            } else break;
        }

        // 👇 FIX: salva subito i pending stops aggiornati
        OrderStorage.savePendingStopOrders(this);

        return triggered;
    }



    private ExecutedTrade createTrade(Order newOrder, Order existingOrder, int price, int size) {
        String buyer = newOrder.getSide() == Side.bid ? newOrder.getUsername() : existingOrder.getUsername();
        String seller = newOrder.getSide() == Side.ask ? newOrder.getUsername() : existingOrder.getUsername();

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
