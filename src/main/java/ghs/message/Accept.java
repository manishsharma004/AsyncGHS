package ghs.message;

/**
 * Sent in response to a {@code Test} message, when the responding process belongs to a different component.
 */
public class Accept extends Message {
    private Integer level;

    public Accept(Integer level) {
        this.level = level;
    }

    @Override
    public String toString() {
        return "Accept{" +
                this.getSender() +
                " ===> " + this.getReceiver() +
                ", level=" + level +
                ", round=" + getRound() +
                '}';
    }
}
