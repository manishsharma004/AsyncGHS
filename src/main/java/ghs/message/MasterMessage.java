package ghs.message;

import floodmax.MessageType;

import java.util.Objects;

public class MasterMessage extends Message implements Comparable {

    private Integer msg;
    private MessageType type;

    public MasterMessage(Integer sender, Integer msg, MessageType type) {
        super(sender, 0);
        this.msg = msg;
        this.type = type;
    }

    public MasterMessage(Integer sender, MessageType type) {
        super(sender, 0);
        this.type = type;
    }

    public Integer getMsg() {
        return msg;
    }

    public void setMsg(Integer msg) {
        this.msg = msg;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return " MasterMessage{" +
                super.toString() +
                ", msg=" + msg +
                ", type=" + type +
                "} ";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        MasterMessage that = (MasterMessage) o;
        return Objects.equals(msg, that.msg) &&
                type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), msg, type);
    }

    @Override
    public int compareTo(Object o) {
        return super.compareTo(o);
    }
}
