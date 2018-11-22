package ghs.message;

import edu.princeton.cs.algs4.Edge;

import java.util.Set;

/**
 * Used to exit the program gracefully and send MST info to master thread.
 */
public class Exit extends Message implements Comparable {
    private Set<Edge> branchEdges;
    private boolean isLeader;
    private Edge coreEdge;

    public Exit(Integer sender) {
        super(sender);
    }

    public Exit(Integer sender, Edge coreEdge, Set<Edge> branchEdges, boolean isLeader) {
        super(sender);
        this.coreEdge = coreEdge;
        this.branchEdges = branchEdges;
        this.isLeader = isLeader;
    }

    public Edge getCoreEdge() {
        return coreEdge;
    }

    public Set<Edge> getBranchEdges() {
        return branchEdges;
    }

    public boolean isLeader() {
        return isLeader;
    }
}
