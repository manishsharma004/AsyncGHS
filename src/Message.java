public class Message {
    Integer sender;
    Integer receiver;
    Integer message;
    MessageType messageType;

    public Message(Integer sender, Integer receiver, Integer message, MessageType messageType) {
        this.sender = sender;
        this.receiver = receiver;
        this.message = message;
        this.messageType = messageType;
    }

    public Integer getSender() {
        return sender;
    }

    private void setSender(Integer sender) {
        this.sender = sender;
    }

    public Integer getReceiver() {
        return receiver;
    }

    private void setReceiver(Integer receiver) {
        this.receiver = receiver;
    }

    public Integer getMessage() {
        return message;
    }

    private void setMessage(Integer message) {
        this.message = message;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    private void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    @Override
    public String toString() {
        return "Message{" +
                "sender=" + sender +
                ", receiver=" + receiver +
                ", message='" + message + '\'' +
                ", messageType='" + messageType + '\'' +
                '}';
    }
}
