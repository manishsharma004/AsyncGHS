public class Message {
    Integer sender;
    Integer receiver;
    String message;
    MessageType type;

    public Message(Integer sender, Integer receiver, String message, MessageType type) {
        this.sender = sender;
        this.receiver = receiver;
        // TODO: message must implement a protocol so that everyone understand what the message means
        this.message = message;
        this.type = type;
    }

    public Integer getSender() {
        return sender;
    }

    public void setSender(Integer sender) {
        this.sender = sender;
    }

    public Integer getReceiver() {
        return receiver;
    }

    public void setReceiver(Integer receiver) {
        this.receiver = receiver;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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
                ", receiver=" + receiver +
                ", message='" + message + '\'' +
                '}';
    }
}
