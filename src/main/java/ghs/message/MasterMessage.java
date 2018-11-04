package ghs.message;

import floodmax.MessageType;

public class MasterMessage extends Message {

    private Integer msg;
    private MessageType type;

    public MasterMessage(Integer sender,Integer msg, MessageType type) {
        super(sender);
        this.msg = msg;
        this.type = type;
    }

    public MasterMessage(Integer sender, MessageType type) {
        super(sender);
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
}
