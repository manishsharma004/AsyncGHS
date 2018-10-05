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
                // System.out.println(this.uid + " can START_ROUND " + msg.message + " now.");
                if (msg.message > this.round) {
                    this.round = msg.message;
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

    synchronized private void handleExploreMsg(Message message) throws InterruptedException {
        /**
         * Handles explore message by updating the max id seen so far.
         */
        int idReceived = message.message;
        if (idReceived > this.maxIdSeen) {
            this.maxIdSeen = idReceived;
        }
    }

    synchronized public void message() throws InterruptedException {
        // send max uid seen so far to all neighbours
        for (Process p : this.neighbors) {
            Message msg = new Message(this.uid, p.getUid(), this.maxIdSeen, MessageType.EXPLORE);
            this.pushToQueue(p, msg);
        }
    }

    private boolean isReadyToTerminate() {
        return this.round == (this.diameter + 1);
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

        if (this.round == this.diameter + 1) {
            if (this.maxIdSeen == this.uid) {
                System.out.println("Electing " + this.uid + " LEADER");
                this.isLeader = true;
            } else {
                this.isLeader = false;
            }
            return;
        }

    }


    @Override
    public void run() {
        try {
            while (true) {
                this.waitUntilMasterStartsNewRound();
                // queue should only have explore messages now
                this.message();
                this.transition();
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