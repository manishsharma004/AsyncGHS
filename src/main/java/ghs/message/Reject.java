package ghs.message;

/**
 * Sent in response to a test message, when the responding process belongs to the same component.
 */
public class Reject extends Message {
    public Reject(Integer sender) {
        super(sender);
    }
}
