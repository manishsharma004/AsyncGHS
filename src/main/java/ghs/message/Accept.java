package ghs.message;

/**
 * Sent in response to a test message, when the responding process belongs to a different component.
 */
public class Accept extends Message {
    /**
     * Level of the sender process.
     */
    private Integer level;

    public Accept(Integer sender, Integer receiver,  Integer level) {
        super(sender, receiver);
        this.level = level;
    }
}
