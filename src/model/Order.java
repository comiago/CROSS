package model;

/**
 * Classe astratta base per rappresentare un ordine.
 * Tutti i tipi di ordini (LIMIT, MARKET, STOP) estenderanno questa classe.
 */
public abstract class Order {
    // ID univoco dell'ordine
    private final long orderId;
    // Nome dell'utente che ha creato l'ordine
    private final String username;
    // Lato dell'ordine: BID (acquisto) o ASK (vendita)
    private final Side side;
    // Quantità dell'ordine
    private int size;
    // Timestamp di creazione dell'ordine (millisecondi)
    private final long timestamp;

    /**
     * Costruttore base dell'ordine.
     *
     * @param orderId ID univoco
     * @param username Nome del cliente
     * @param side Lato dell'ordine (BID/ASK)
     * @param size Quantità dell'ordine
     */
    public Order(long orderId, String username, Side side, int size) {
        this.orderId = orderId;
        this.username = username;
        this.side = side;
        this.size = size;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Restituisce il tipo di ordine (LIMIT, MARKET, STOP)
     */
    public abstract OrderType getOrderType();

    // --- Getters ---
    public long getOrderId() { return orderId; }
    public String getUsername() { return username; }
    public Side getSide() { return side; }
    public int getSize() { return size; }
    public long getTimestamp() { return timestamp; }

    // --- Setter solo per size, utile per ordini parzialmente eseguiti ---
    public void setSize(int size) { this.size = size; }

    @Override
    public String toString() {
        return "Order{" +
                "orderId=" + orderId +
                ", username='" + username + '\'' +
                ", side=" + side +
                ", size=" + size +
                ", timestamp=" + timestamp +
                ", orderType=" + getOrderType() +
                '}';
    }
}
