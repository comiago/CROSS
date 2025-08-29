package model;

/**
 * Rappresenta un ordine di tipo LIMIT.
 * Contiene un prezzo fisso al quale il compratore o venditore è disposto a scambiare.
 */
public class LimitOrder extends Order {

    /** Prezzo fissato per l'ordine limit */
    private final int price;

    /**
     * Costruisce un ordine limit.
     *
     * @param orderId ID univoco dell'ordine
     * @param user Username del cliente
     * @param side Lato dell'ordine (BID/ASK)
     * @param size Quantità dell'ordine
     * @param price Prezzo fissato
     */
    public LimitOrder(long orderId, String user, Side side, int size, int price) {
        super(orderId, user, side, size);
        this.price = price;
    }

    @Override
    public OrderType getOrderType() {
        return OrderType.LIMIT;
    }

    public int getPrice() {
        return price;
    }

    @Override
    public String toString() {
        return "LimitOrder{" +
                "orderId=" + getOrderId() +
                ", user='" + getUsername() + '\'' +
                ", side=" + getSide() +
                ", size=" + getSize() +
                ", price=" + price +
                '}';
    }
}
