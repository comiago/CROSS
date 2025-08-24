package model;

/**
 * Rappresenta un ordine di tipo MARKET.
 */
public final class MarketOrder extends Order {

    public MarketOrder(long orderId, Client client, Side side, int size) {
        super(orderId, client, side, size);
    }

    @Override
    public OrderType getOrderType() {
        return OrderType.MARKET;
    }
}
