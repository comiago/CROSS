package model;

/**
 * Rappresenta un ordine di tipo LIMIT con prezzo fisso.
 */
public class LimitOrder extends Order {
    private final int price;

    public LimitOrder(long orderId, Client user, Side side, int size, int price) {
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
}
