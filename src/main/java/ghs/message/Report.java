package ghs.message;

import edu.princeton.cs.algs4.Edge;

import java.util.Objects;

/**
 * Represents a report message, that convergecasts information about mwoe to the leader.
 */
public class Report extends Message implements Comparable {
    /**
     * Weight of the mwoe
     */
    private Edge mwoe;

    public Report(Edge mwoe) {
        this.mwoe = mwoe;
    }

    public Edge getMwoe() {
        return mwoe;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Report report = (Report) o;
        return Objects.equals(mwoe, report.mwoe);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), mwoe);
    }

    @Override
    public int compareTo(Object o) {
        return super.compareTo(o);
    }

    @Override
    public String toString() {
        return "Report{" +
                this.getSender() +
                " ===> " + this.getReceiver() +
                ", mwoe=" + mwoe +
                '}';
    }
}
