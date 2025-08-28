package model;

public class ExecutedTrade {
    private String buyer;
    private String seller;
    private OrderType orderType;
    private Side initiatorSide; // Lato dell'ordine che ha innescato il trade (ask o bid)
    private int size;
    private int price;
    private long timestamp;

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

    // Getters (eventualmente anche setters, se usi Gson)
    public String getBuyer() { return buyer; }
    public String getSeller() { return seller; }
    public OrderType getOrderType() { return orderType; }
    public Side getInitiatorSide() { return initiatorSide; }
    public int getSize() { return size; }
    public int getPrice() { return price; }
    public long getTimestamp() { return timestamp; }
}
