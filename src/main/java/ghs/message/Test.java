package ghs.message;

import edu.princeton.cs.algs4.Edge;

/**
 * Represents a test message a process sends to its neighbor. This is part of the procedure in which the current process
 * searches for its mwoe.
 */
public class Test extends Message implements Comparable {
    /**
     * Weight of the core edge of the sender's component
     */
    Edge coreEdge;

    /**
     * Level of the sender
     */
    private Integer level;

    public Test(Integer sender, Integer receiver, Edge coreEdge, Integer level) {
        super(sender, receiver);
        this.coreEdge = coreEdge;
        this.level = level;
    }

    public Integer getLevel() {
        return level;
    }

    public Edge getCoreEdge() {
        return coreEdge;
    }

    @Override
    public int compareTo(Object o) {
        return super.compareTo(o);
    }

    @Override
    public String toString() {
        return "Test{" +
                this.getSender() +
                " ===> " + this.getReceiver() +
                ", coreEdge=" + coreEdge +
                ", level=" + level +
                '}';
    }
}
