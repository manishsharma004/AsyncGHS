package ghs.message;

import java.util.Objects;

/**
 * Represents a test message a process sends to its neighbor. This is part of the procedure in which the current process
 * searches for its mwoe.
 */
public class Test extends Message implements Comparable {
    /**
     * Weight of the core edge of the sender's component
     */
    private Integer coreEdgeWeight;

    /**
     * Level of the sender
     */
    private Integer level;

    public Test(Integer sender, Integer receiver,  Integer round, Integer coreEdgeWeight, Integer level) {
        super(sender, receiver, round);
        this.coreEdgeWeight = coreEdgeWeight;
        this.level = level;
    }

    public Test(Integer sender, Integer receiver, Integer coreEdgeWeight, Integer level) {
        super(sender, receiver);
        this.coreEdgeWeight = coreEdgeWeight;
        this.level = level;
    }


    @Override
    public int compareTo(Object o) {
        return super.compareTo(o);
    }

    @Override
    public String toString() {
        return "Test{" + super.toString() +
                ", coreEdgeWeight=" + coreEdgeWeight +
                ", level=" + level +
                "} " ;
    }
}
