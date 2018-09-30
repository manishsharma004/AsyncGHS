import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class Process extends Thread {
    int uid; // TODO: uid can be anything that can be compared
    int maxIdSeen = -1;
    int status = -1; // 0 for non-leader, 1 for leader
    int round = 0;  // initially round is 0
    int diameter;

    BlockingQueue<Message> queue = new LinkedBlockingDeque<>(10);
    Map<Integer, List<Process>> neighbors;

    public Process(String name, int uid) {
        super(name);
        this.uid = uid;
    }

    public Process(String name, int uid, int diameter, HashMap<Integer, List<Process>> neighbors) {
        super(name);
        this.uid = uid;
        this.neighbors = neighbors;
    }

    public void setDiameter(int diameter) {
        this.diameter = diameter;
    }

    public void setNeighbors(Map<Integer, List<Process>> neighbors) {
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

    public int getUid() {
        return uid;
    }

    synchronized public void message() throws InterruptedException {
        if (this.round <= this.diameter) {
            // send max uid seen so far to all neighbours
            List<Process> outNeighbors = this.neighbors.get(this.uid);
            for (Process p : outNeighbors) {
                Message msg = new Message(this.uid, p.getUid(), this.maxIdSeen);
                this.pushToQueue(p, msg);
            }
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
            if (inMsg.message > maxIdSeen) {
                maxIdSeen = inMsg.message;
            }
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                if (this.status >= 0) {
                    // TODO: print leader
                    break;
                } else {
                    this.message();
                    this.transition();
                }
            }
        } catch (InterruptedException e) {
        }
    }
}