package ghs.message;

/**
 * A connect message is sent across the mwoe of a component C when that component attempts to combine with another
 * component.
 */
public class Connect extends Message {
    /**
     * Level of the component C of the process that sends a connect message.
     */
    private Integer level;

    public Connect(Integer sender, Integer receiver,  Integer level) {
        super(sender, receiver);
        this.level = level;
    }
}
