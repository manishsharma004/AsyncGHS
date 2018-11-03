package ghs.message;

/**
 * Represents a test message a process sends to its neighbor. This is part of the procedure in which the current process
 * searches for its mwoe.
 */
public class Test extends Message {
    /**
     * Weight of the core edge of the sender's component
     */
    private Integer coreEdgeWeight;

    /**
     * Level of the sender
     */
    private Integer level;

    public Test(Integer sender, Integer coreEdgeWeight, Integer level) {
        super(sender);
        this.coreEdgeWeight = coreEdgeWeight;
        this.level = level;
    }
}
