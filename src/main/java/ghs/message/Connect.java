package ghs.message;

import edu.princeton.cs.algs4.Edge;

import java.util.Objects;

/**
 * A connect message is sent across the mwoe of a component C when that component attempts to combine with another
 * component.
 */
public class Connect extends Message {
    /**
     * Level of the component C of the process that sends a connect message.
     */
    private Integer level;
    private Edge mwoe;

    public Connect(Integer level, Edge mwoe) {
        this.level = level;
        this.mwoe = mwoe;
    }

    public Connect(Integer sender, Integer receiver, Integer level) {
        super(sender, receiver);
        this.level = level;
    }

    public Integer getLevel() {
        return level;
    }

    public Edge getMwoe() {
        return mwoe;
    }

    @Override
    public String toString() {
        return "Connect{" +
                this.getSender() +
                " ===> " + this.getReceiver() +
                ", level=" + level +
                ", mwoe=" + mwoe +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Connect connect = (Connect) o;
        return level.equals(connect.level) &&
                mwoe.equals(connect.mwoe);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), level, mwoe);
    }
}
