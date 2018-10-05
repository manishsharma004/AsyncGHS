import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class Process extends Thread {
    int uid; // TODO: uid can be anything that can be compared, must implement comparator
    int maxIdSeen = -1;
    int status = -1; // 0 for non-leader, 1 for leader
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
    }

    public Process(String name, int uid, List<Process> neighbors) {
        super(name);
        this.uid = uid;
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

    synchronized public void message() throws InterruptedException {
        if (this.round <= this.diameter) {
            // send max uid seen so far to all neighbours
            for (Process p : this.neighbors) {
                Message msg = new Message(this.uid, p.getUid(), String.valueOf(this.maxIdSeen), MessageType.EXPLORE);
                this.pushToQueue(p, msg);
            }
        }
    }

    synchronized private void handleExploreMsg(Message message) throws InterruptedException {
        /**
         * Handles explore message by updating the max id seen so far.
         */
        int idReceived = Integer.parseInt(message.message);
        if (idReceived > this.maxIdSeen) {
            this.maxIdSeen = idReceived;
        }
    }

    synchronized public void transition() throws InterruptedException {
        this.round += 1;
        // TODO: write convergecast that doesn't assume knowledge of diameter
        if (round == this.diameter) {
            if (this.maxIdSeen == this.uid) {
                this.status = 1;
            } else {
                this.status = 0;
            }
        }
        // get all messages from neighbours (i.e. current process's queue) and update maxIdSeen
        Message inMsg;
        while (!queue.isEmpty()) {
            inMsg = queue.take();
            // TODO: handle all ACKs and NACKs also, when diameter is unknown
            switch (inMsg.getType()) {
                case EXPLORE:
                    this.handleExploreMsg(inMsg);
                    break;
            }
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                if (this.status >= 0) {
                    // TODO: send message to master notifying I am the leader
                    break;
                } else {
                    this.message();
                    this.transition();
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