package model;

/**
 * Rappresenta un trade eseguito tra due ordini.
 * Contiene informazioni sul buyer, seller, tipo ordine e dettagli di esecuzione.
 */
public class ExecutedTrade {

    /** Username del compratore */
    private final String buyer;

    /** Username del venditore */
    private final String seller;

    /** Tipo di ordine (LIMIT, MARKET, STOP, etc.) */
    private final OrderType orderType;

    /** Lato dell'ordine che ha innescato il trade (ask o bid) */
    private final Side initiatorSide;

    /** Quantità di asset scambiata */
    private final int size;

    /** Prezzo a cui è stato eseguito il trade */
    private final int price;

    /** Timestamp dell'esecuzione in millisecondi */
    private final long timestamp;

    public ExecutedTrade(String buyer, String seller, OrderType orderType,
                         Side initiatorSide, int size, int price, long timestamp) {
        this.buyer = buyer;
        this.seller = seller;
        this.orderType = orderType;
        this.initiatorSide = initiatorSide;
        this.size = size;
        this.price = price;
        this.timestamp = timestamp;
    }

    // --- Getters ---
    public String getBuyer() { return buyer; }
    public String getSeller() { return seller; }
    public OrderType getOrderType() { return orderType; }
    public Side getInitiatorSide() { return initiatorSide; }
    public int getSize() { return size; }
    public int getPrice() { return price; }
    public long getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return "ExecutedTrade{" +
                "buyer='" + buyer + '\'' +
                ", seller='" + seller + '\'' +
                ", orderType=" + orderType +
                ", initiatorSide=" + initiatorSide +
                ", size=" + size +
                ", price=" + price +
                ", timestamp=" + timestamp +
                '}';
    }
}
