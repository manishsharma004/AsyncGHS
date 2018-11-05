package ghs.message;

import java.util.LinkedList;

/**
 * Represents a report message, that convergecasts information about mwoe to the leader.
 */
public class Report extends Message {
    /**
     * Weight of the mwoe
     */
    private Integer mwoeWeight;

    /**
     * Path to the process in the component adjacent to mwoe, first element should be the leader and the last should be
     * the process adjacent to the mwoe.
     */
    private LinkedList<Integer> pathToMWOE;

    public Report(Integer sender, Integer receiver,  Integer mwoeWeight) {
        super(sender, receiver);
        this.mwoeWeight = mwoeWeight;
        this.pathToMWOE = new LinkedList<>();
    }

    /**
     * Updates the path to MWOE so that the leader can back-track it.
     *
     * @param pid id of the process that sent the mwoe
     */
    public void updatePath(Integer pid) {
        this.pathToMWOE.addFirst(pid);
    }
}
