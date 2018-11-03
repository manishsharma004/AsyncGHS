package ghs.message;

/**
 * Represents the format of a message to be sent in the network.
 */
public abstract class Message {
    /**
     * ID of the sender process
     */
    private Integer sender;

    public Message(Integer sender) {
        this.sender = sender;
    }
}
