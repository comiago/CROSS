package model;

/**
 * Rappresenta un ordine di tipo MARKET.
 */
public final class MarketOrder extends Order {

    public MarketOrder(long orderId, String username, Side side, int size) {
        super(orderId, username, side, size);
    }

    @Override
    public OrderType getOrderType() {
        return OrderType.MARKET;
    }
}
