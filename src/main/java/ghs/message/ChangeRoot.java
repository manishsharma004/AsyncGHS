package ghs.message;

import edu.princeton.cs.algs4.Edge;

/**
 * A {@code ChangeRoot} message is sent from the leader of the component toward the component process that is adjacent
 * to the component's mwoe, after the mwoe has been determined.
 */
public class ChangeRoot extends Message {
    private Edge mwoe;

    public ChangeRoot(Edge mwoe) {
        this.mwoe = mwoe;
    }

    public Edge getMwoe() {
        return mwoe;
    }

    @Override
    public String toString() {
        return "ChangeRoot{" +
                this.getSender() +
                " ===> " + this.getReceiver() +
                ", mwoe=" + mwoe +
                '}';
    }
}
