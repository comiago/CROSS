package model;

/**
 * Rappresenta un ordine di tipo STOP.
 * L'ordine diventa un MarketOrder quando il prezzo di mercato raggiunge lo stopPrice.
 */
public class StopOrder extends Order {
    private final int stopPrice; // Prezzo al quale l'ordine viene attivato

    public StopOrder(long orderId, String client, Side side, int size, int stopPrice) {
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
