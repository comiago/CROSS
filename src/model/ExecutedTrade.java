package model;

public class ExecutedTrade {
    private transient Client buyer;
    private transient Client seller;
    private String buyerName;
    private String sellerName;
    private Side initiatorSide; // Lato dell'ordine che ha innescato il trade (ask o bid)
    private int size;
    private int price;
    private long timestamp;

    public ExecutedTrade(Client buyer, Client seller,
                         Side initiatorSide, int size, int price, long timestamp) {
        this.buyer = buyer;
        this.seller = seller;
        this.buyerName = buyer.getUsername();
        this.sellerName = seller.getUsername();
        this.initiatorSide = initiatorSide;
        this.size = size;
        this.price = price;
        this.timestamp = timestamp;
    }

    // Getters (eventualmente anche setters, se usi Gson)
    public Client getBuyer() { return buyer; }
    public Client getSeller() { return seller; }
    public Side getInitiatorSide() { return initiatorSide; }
    public int getSize() { return size; }
    public int getPrice() { return price; }
    public long getTimestamp() { return timestamp; }
}
