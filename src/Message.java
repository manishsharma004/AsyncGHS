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

    public Integer getSender() {
        return sender;
    }

    public void setSender(Integer sender) {
        this.sender = sender;
    }

    public Integer getMaxId() {
        return maxId;
    }

    public void setMaxId(Integer maxId) {
        this.maxId = maxId;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
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