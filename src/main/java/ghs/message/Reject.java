package ghs.message;

/**
 * Sent in response to a {@code Test} message, when the responding process belongs to the same component.
 */
public class Reject extends Message {
    public Reject() {
    }

    @Override
    public String toString() {
        return "Reject{" +
                this.getSender() +
                " ===> " + this.getReceiver() +
                "}";
    }
}
