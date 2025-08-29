package model;

/**
 * Rappresenta un ordine di tipo MARKET.
 * Viene eseguito immediatamente al miglior prezzo disponibile nel book.
 */
public final class MarketOrder extends Order {

    /**
     * Costruisce un ordine market.
     *
     * @param orderId ID univoco dell'ordine
     * @param client Username del cliente
     * @param side Lato dell'ordine (BID/ASK)
     * @param size Quantità dell'ordine
     */
    public MarketOrder(long orderId, String client, Side side, int size) {
        super(orderId, client, side, size);
    }

    @Override
    public OrderType getOrderType() {
        return OrderType.MARKET;
    }

    @Override
    public String toString() {
        return "MarketOrder{" +
                "orderId=" + getOrderId() +
                ", client='" + getUsername() + '\'' +
                ", side=" + getSide() +
                ", size=" + getSize() +
                '}';
    }
}
