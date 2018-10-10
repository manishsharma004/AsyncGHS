public enum MessageType {
    ACK, // positive acknowledgement from a process or accept
    NACK, // negative acknowledgement from a process or reject
    EXPLORE, // explore maxId sent by a process to another, in this case to compare UIDs
    END_ROUND, // process sends to master to notify that it has completed its round
    START_ROUND, // master sends to process to give green signal to start round
    TERMINATE, // process sends this to master to signal that it is done and can shut down,
    DUMMY   // represents a NULL maxId, i.e. when no action is needed
}