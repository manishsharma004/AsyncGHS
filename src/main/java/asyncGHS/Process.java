package asyncGHS;


import floodmax.MessageType;
import ghs.message.MasterMessage;
import ghs.message.Message;
import ghs.message.Test;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.concurrent.*;

public class Process extends Thread{

    private Logger log = Logger.getLogger(this.getName());
    public static Random random = new Random();
    // states
    private int uid;
    private int round = 0;  // initially round is 0, but everything starts from round 1 (master sends this)
    private int parentId = -1;
    private boolean selfKill = false;

    private CyclicBarrier barrier;

    BlockingQueue<Message> inqueue = new PriorityBlockingQueue<>(30, Message::compareTo);
    BlockingQueue<Message> queue = new PriorityBlockingQueue<>(30, Message::compareTo);
    BlockingQueue<Message> masterQueue = new LinkedBlockingDeque<>(10);
    private MasterThread master;

    //neighbor and its process
    private Map<Integer, Process> vertexToProcess = new HashMap<>();
    private Map<Integer, Integer> vertexToDelay = new HashMap<>();


    private HashSet<NeighborObject> neighbors = new HashSet<>();

    public Process(String name, int uid, CyclicBarrier barrier) {
        super(name);
        this.uid = uid;
        this.barrier = barrier;
    }
    public void setMaster(MasterThread master) {
        this.master = master;
    }

    public int getUid() {
        return this.uid;
    }


    public void setNeighborProcesses(HashMap<NeighborObject, Process> neighborProcesses) {
        for (NeighborObject p : neighborProcesses.keySet()) {
            neighbors.add(p);
            vertexToProcess.put(p.getId(), neighborProcesses.get(p));
            vertexToDelay.put(p.getId(), 0);
        }
    }

    private void waitUntilMasterStartsNewRound() throws InterruptedException {
        while (true) {
            MasterMessage msg = ((MasterMessage) masterQueue.take());
            if (msg.getType().equals(MessageType.START_ROUND)) {
                if (msg.getMsg() > round) {
                    round = msg.getMsg();
                } else {
                    // This should never happen
                    throw new InterruptedException("Received round < current round");
                }
                return;
            } else if (msg.getType().equals(MessageType.KILL)) {
                selfKill = true;
                break;
            } else {
                masterQueue.add(msg);
            }
        }
    }

    private Integer getDelay() {
        return 1 + random.nextInt(19);
    }

    private void checkInQueue() throws InterruptedException {
        while (!inqueue.isEmpty() && inqueue.peek().getRound() <= round) {
            Message m = inqueue.take();
            pushToQueue(vertexToProcess.get(m.getReceiver()), m);
        }
    }

    private void pushToInQueue(Message m)
    {
        inqueue.add(m);
    }

    private int getRound(int receiver) {
        return vertexToDelay.get(receiver) + getDelay();
    }

    private void pushToQueue(Process p, Message m) {
        p.queue.add(m);
    }

    private void pushToQueue(MasterThread p, MasterMessage m) {
        p.queue.add(m);
    }

    private void sendRoundCompletionToMaster() {
        pushToQueue(master, new MasterMessage(uid, round, MessageType.END_ROUND));
    }

    private void sendTerminationToMaster() {
        pushToQueue(master, new MasterMessage(uid, parentId, MessageType.TERMINATE));
    }

    private void sendMessages(HashSet<NeighborObject> neighbors) {
        for (NeighborObject neighbor : neighbors) {
            int round = getRound(neighbor.id);
            Message testMsg = new Test(uid, neighbor.id, round, 0, 1);
            pushToInQueue(testMsg);
        }
    }

    private void message() {
        // defining messages
        if (round <= 2) {
            sendMessages(neighbors);
        }
    }

    private void handleMessages() throws InterruptedException {
        Message msg;
        while (!queue.isEmpty()) {
            msg = queue.take();
            log.info(msg);
        }
    }

    private void transition() throws InterruptedException, BrokenBarrierException {
        barrier.await();    // wait until all threads have sent explore messages
        checkInQueue();
        barrier.await();
        handleMessages();
    }

    @Override
    public void run() {
        try {
            while (true) {
                waitUntilMasterStartsNewRound();
                if (selfKill) {
                    break;
                }

                message();
                transition();

                if (round == 30) {
                    sendTerminationToMaster();
                }
                else {
                    sendRoundCompletionToMaster();
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (BrokenBarrierException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return "Process{" +
                "uid=" + uid +
                "} " + super.toString();
    }
}
