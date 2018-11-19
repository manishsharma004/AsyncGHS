package ghs.message;

/**
 * Sent in response to a test message, when the responding process belongs to the same component.
 */
public class Reject extends Message {
    public Reject() {
    }

    public Reject(Integer sender) {
        super(sender);
    }

    public Reject(Integer sender, Integer receiver) {
        super(sender, receiver);
    }

    @Override
    public String toString() {
        return "Reject{" +
                this.getSender() +
                " ===> " + this.getReceiver() +
                "}";
    }
}
