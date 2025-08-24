package model;


/**
 * Classe base astratta per rappresentare un ordine.
 */
public abstract class Order {
    private final long orderId;
    private final transient Client user;
    private final String username;
    private final Side side;
    private int size;
    private final long timestamp;

    public Order(long orderId, Client user, Side side, int size) {
        this.orderId = orderId;
        this.user = user;
        this.username = user.getUsername();
        this.side = side;
        this.size = size;
        this.timestamp = System.currentTimeMillis();
    }

    public abstract OrderType getOrderType();

    public long getOrderId() {
        return orderId;
    }

    public Client getUser() {
        return user;
    }

    public Side getSide() {
        return side;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "Order{" +
                "orderId=" + orderId +
                ", username='" + username + '\'' +
                ", side=" + side +
                ", size=" + size +
                ", timestamp=" + timestamp +
                ", orderType=" + getOrderType() +
                '}';
    }
}
