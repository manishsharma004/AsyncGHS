import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class Process extends Thread {
    int uid; // TODO: uid can be anything that can be compared, must implement comparator
    int maxIdSeen;
    boolean isLeader;
    int round = 0;  // initially round is 0
    int diameter;
    MasterThread master;    // for synchronization
    BlockingQueue<Message> queue = new LinkedBlockingDeque<>(10);
    List<Process> neighbors;    // a process has local information only
    boolean isUpdated = false;
    boolean isLeaf = false;
    int parentId = -1;
    HashSet<Integer> children = new HashSet<>();
    HashSet<Integer> others = new HashSet<>();

    public void setMaster(MasterThread master) {
        this.master = master;
    }

    public Process(String name, int uid) {
        super(name);
        this.uid = uid;
        this.maxIdSeen = uid;
    }

    public Process(String name, int uid, List<Process> neighbors) {
        super(name);
        this.uid = uid;
        this.maxIdSeen = uid;
        this.neighbors = neighbors;
    }

    public void setDiameter(int diameter) {
        this.diameter = diameter;
    }

    public void setNeighbors(List<Process> neighbors) {
        this.neighbors = neighbors;
    }

    @Override
    public synchronized void start() {
        super.start();
    }

    /**
     * Sends message to another process
     *
     * @param p process to which the current process sends a message
     * @param m message to send
     * @return whether the message was sent or not
     */
    private synchronized boolean pushToQueue(Process p, Message m) {
        return p.queue.add(m);
    }

    private synchronized boolean pushToQueue(MasterThread p, Message m) {
        return p.queue.add(m);
    }

    synchronized public boolean sendMessageToMaster(Message msg) {
        return pushToQueue(this.master, msg);
    }


    public int getUid() {
        return uid;
    }

    public synchronized void waitUntilMasterStartsNewRound() throws InterruptedException {
        while (true) {
            Message msg = this.queue.take();
            if (msg.getType().equals(MessageType.START_ROUND)) {

                if (msg.message > this.round) {
                    this.round = msg.message;
//                    System.out.println(this.getName() + " has started round " + this.round);
                } else {
                    // This should never happen
                    throw new InterruptedException("Received round < current round");
                }
                return;
            } else {
                this.queue.add(msg);
            }
        }
    }

    public synchronized void sendRoundCompletionToMaster() {
        // System.out.println(this.uid + " sending END_ROUND message to master");
        this.sendMessageToMaster(new Message(this.uid, 0, this.round, MessageType.END_ROUND));
    }

    public synchronized void sendTerminationToMaster() {
        System.out.println(this.getName() + " sending TERMINATE to master");
        this.sendMessageToMaster(new Message(this.uid, 0, 0, MessageType.TERMINATE));
    }

    synchronized public void message() throws InterruptedException {
        // send max uid seen so far to all neighbours
        for (Process p : this.neighbors) {
            Message msg = new Message(this.uid, p.getUid(), this.maxIdSeen, MessageType.EXPLORE);
//            System.out.println("Sending EXPLORE message to Neighbors of Thread " + this.getName() +
//                    " maxUid : " + this.maxIdSeen + " to " + p.getUid());
            this.pushToQueue(p, msg);
        }
    }

    synchronized  public void messageAcknowledge() throws InterruptedException {
        if (isLeaf && parentId != -1) {
            //send ACK to parent
            //send NACK to rest
            for (Process p : this.neighbors) {
                if (p.uid == parentId) {
                    this.pushToQueue(p, new Message(this.uid, p.getUid(), this.maxIdSeen, MessageType.ACK));
                } else {
                    this.pushToQueue(p, new Message(this.uid, p.getUid(), this.maxIdSeen, MessageType.NACK));
                }
            }
        }
        else if (!isLeaf && parentId != -1 && isUpdated) {
            //send DUMMY to Parent
            //send NACK to res
            for (Process p : this.neighbors) {
                if (p.uid == parentId) {
                    this.pushToQueue(p, new Message(this.uid, p.getUid(), this.maxIdSeen, MessageType.DUMMY));
                } else {
                    this.pushToQueue(p, new Message(this.uid, p.getUid(), this.maxIdSeen, MessageType.NACK));
                }
            }
        }
        else {
            //send NACK to all
            for (Process p : this.neighbors) {
                this.pushToQueue(p, new Message(this.uid, p.getUid(), this.maxIdSeen, MessageType.NACK));
            }
        }
    }

    private boolean isReadyToTerminate() {
        if (this.isLeader) {
            System.out.println("Leader is Elected : " + this.maxIdSeen);
        }
        return this.isLeader;
    }

    synchronized private void handleExploreMsg(Message message) throws InterruptedException {
        /**
         * Handles explore message by updating the max id seen so far.
         */
        //System.out.println("Received " + message);
        int idReceived = message.message;
        if (idReceived > this.maxIdSeen) {
            this.maxIdSeen = idReceived;
            this.isUpdated = true;
            this.parentId = message.sender;
            System.out.println("MaxId Updated in " + this.getName() + " by " + this.maxIdSeen + " parent: " + this.parentId);
        }
    }

    synchronized private void handleAckMsg(Message message) throws InterruptedException {
        /**
         * Handles ACK message by  adding sender's id to children list
         */
        //System.out.println("Received " + message);
        System.out.println("Process " + message.sender + " is child of " + this.getName());
        this.children.add(message.sender);
    }

    synchronized private void handleNackMsg(Message message) throws InterruptedException {
        /**
         * Handles Nack by adding it to others list but
         *  if sender is already a child and has max value greater than maxuid then he is not child anymore
         */
        //System.out.println("Received " + message);
        if (message.message > this.maxIdSeen) {
            this.children.remove(message.sender);
        }
        this.others.add(message.sender);
    }


    synchronized private void handleDummyMsg(Message message) throws InterruptedException {
        /**
         * Handles DUMMY message by  adding sender's id to others list
         */
        //System.out.println("Received " + message);
        //this.others.add(message.sender);
    }

    synchronized public void handleMessages (Message inMsg)  throws InterruptedException {
        switch (inMsg.getType()) {
            case EXPLORE:
                handleExploreMsg(inMsg);
                break;
            case ACK:
                handleAckMsg(inMsg);
                break;
            case NACK:
                handleNackMsg(inMsg);
                break;
            case DUMMY:
                handleDummyMsg(inMsg);
                break;
            default:
                break;
        }
    }

    synchronized public void transition() throws InterruptedException {
        // wait until messages from all neighbors have arrived
        while (true) {
            if (this.queue.size() == this.neighbors.size()) {
                break;
            }
        }
        // System.out.println(this.getName() + " received messages from all " + this.neighbors.size() + " neighbors");

        // get messages from all neighbours (i.e. current process's queue) and update maxIdSeen
        Message inMsg;
        while (!queue.isEmpty()) {
            inMsg = queue.take();
            switch (inMsg.getType()) {
                case EXPLORE:
                    handleExploreMsg(inMsg);
                    break;
                default:
                    break;
            }
        }
    }

    synchronized public void transitionAcknowledge()  throws InterruptedException {
        // wait until messages from all neighbors have arrived
        while (true) {
            if (this.queue.size() == this.neighbors.size()) {
                break;
            }
        }
        // System.out.println(this.getName() + " received messages from all " + this.neighbors.size() + " neighbors");

        // get messages from all neighbours (i.e. current process's queue) and update maxIdSeen
        Message inMsg;
        while (!queue.isEmpty()) {
            inMsg = queue.take();
            switch (inMsg.getType()) {
                case EXPLORE:
                    handleExploreMsg(inMsg);
                    break;
                case ACK:
                    handleAckMsg(inMsg);
                    break;
                case NACK:
                    handleNackMsg(inMsg);
                    break;
                case DUMMY:
                    handleDummyMsg(inMsg);
                    break;
                default:
                    break;
            }
        }

        if (this.children.size() == this.neighbors.size())
        {
            this.isLeader = true;
        }
        else {
            this.isLeader = false;
        }
        if (this.others.size()  ==  this.neighbors.size()) {
            this.isLeaf = true;
            //System.out.println(this.getName() + " is leaf node in this round " + this.round);
        }
        else {
            this.isLeaf = false;
        }
        isUpdated = false;
        this.others = new HashSet<>();
    }


    @Override
    public void run() {
        try {
            while (true) {
                this.waitUntilMasterStartsNewRound();
                // queue should only have explore messages now
                if (this.round % 2 == 1) {
                    this.message();
                    this.transition();
                }
                else {
                    this.messageAcknowledge();
                    this.transitionAcknowledge();
                }
                
                if (!isReadyToTerminate()) {
                    this.sendRoundCompletionToMaster();
                } else {
                    this.sendTerminationToMaster();
                    throw new InterruptedException(this.getName() + " shutting down");
                }
            }
        } catch (InterruptedException e) {
        }
    }

    @Override
    public String toString() {
        return "Process{" +
                "uid=" + uid +
                '}';
    }
}