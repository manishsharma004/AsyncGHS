package ghs.message;

import edu.princeton.cs.algs4.Edge;

/**
 * Represents a {@code Test} message a process sends on its basic edge.
 *
 * <p>This is part of the test-accept-reject protocol in which the current process searches for its mwoe.</p>
 */
public class Test extends Message implements Comparable {
    Edge coreEdge;
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
                ", round=" + getRound() +
                '}';
    }
}
