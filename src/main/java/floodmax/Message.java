package floodmax;

public class Message {
    Integer sender;
    Integer maxId;
    MessageType type;

    public Message(Integer sender, Integer maxId, MessageType type) {
        this.sender = sender;
        this.maxId = maxId;
        this.type = type;
    }

    public Message(Integer sender, MessageType type) {
        this.sender = sender;
        this.type = type;
    }

    public Integer getMaxId() {
        return maxId;
    }

    public MessageType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "Message{" +
                "sender=" + sender +
                ", maxId=" + maxId +
                ", type=" + type +
                '}';
    }
}