package model;

public class StopOrder extends Order {
    private final int stopPrice;

    public StopOrder(long orderId, Client client, Side side, int size, int stopPrice) {
        super(orderId, client, side, size);
        this.stopPrice = stopPrice;
    }

    @Override
    public OrderType getOrderType() {
        return OrderType.STOP;
    }

    public int getStopPrice() {
        return stopPrice;
    }
}
