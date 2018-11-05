package ghs.message;

import java.util.LinkedList;
import java.util.Objects;

/**
 * Represents a report message, that convergecasts information about mwoe to the leader.
 */
public class Report extends Message implements Comparable {
    /**
     * Weight of the mwoe
     */
    private Integer mwoeWeight;

    /**
     * Path to the process in the component adjacent to mwoe, first element should be the leader and the last should be
     * the process adjacent to the mwoe.
     */
    private LinkedList<Integer> pathToMWOE;

    public Report(Integer sender, Integer receiver, Integer round, Integer mwoeWeight) {
        super(sender, receiver, round);
        this.mwoeWeight = mwoeWeight;
        this.pathToMWOE = new LinkedList<>();
    }

    /**
     * Updates the path to MWOE so that the leader can back-track it.
     *
     * @param pid id of the process that sent the mwoe
     */
    public void updatePath(Integer pid)
    {
        this.pathToMWOE.addFirst(pid);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Report report = (Report) o;
        return Objects.equals(mwoeWeight, report.mwoeWeight) &&
                Objects.equals(pathToMWOE, report.pathToMWOE);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), mwoeWeight, pathToMWOE);
    }

    @Override
    public int compareTo(Object o) {
        return super.compareTo(o);
    }
}
