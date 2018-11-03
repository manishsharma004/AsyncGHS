package ghs.message;

/**
 * Represents an initiate message, broadcast by the leader of the component to all processes in its component to start
 * searching for the MWOE.
 */
public class Initiate extends Message {
    /**
     * Level of the sender's component
     */
    private Integer level;

    /**
     * Core edge weight of the sender's component
     */
    private Integer coreEdgeWeight;

    /**
     * ID of leader of sender's component
     */
    private Integer leader;

    public Initiate(Integer sender, Integer level, Integer coreEdgeWeight, Integer leader) {
        super(sender);
        this.level = level;
        this.coreEdgeWeight = coreEdgeWeight;
        this.leader = leader;
    }
}
