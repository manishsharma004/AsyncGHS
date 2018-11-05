package ghs.message;

/**
 * A changeroot message is sent from the leader of the component toward the component process that is adjacent to the
 * component's mwoe, aftet the mwoe has been determined.
 */
public class ChangeRoot extends Message {
    /**
     * ID of the process adjacent to the mwoe, i.e. the potential new leader of the combined component.
     */
    private Integer newLeader;

    public ChangeRoot(Integer sender, Integer receiver, Integer newLeader) {
        super(sender, receiver);
        this.newLeader = newLeader;
    }

}
