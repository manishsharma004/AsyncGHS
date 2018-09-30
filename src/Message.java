public class Message {
    Integer sender;
    Integer receiver;
    Integer message;

    public Message(Integer sender, Integer receiver, Integer message) {
        this.sender = sender;
        this.receiver = receiver;
        this.message = message;
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

    @Override
    public String toString() {
        return "Message{" +
                "sender=" + sender +
                ", receiver=" + receiver +
                ", message='" + message + '\'' +
                '}';
    }
}
