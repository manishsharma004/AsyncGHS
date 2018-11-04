package asyncGHS;


import floodmax.MessageType;
import ghs.message.MasterMessage;
import ghs.message.Message;
import ghs.message.Report;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.PriorityBlockingQueue;

public class Process extends Thread{

    private Logger log = Logger.getLogger(this.getName());

    // states
    private int uid;
    private int round = 0;  // initially round is 0, but everything starts from round 1 (master sends this)
    private int parentId = -1;
    private boolean isLeader;
    private boolean isReadyToTerminate = false;
    private boolean selfKill = false;

    private CyclicBarrier barrier;

    BlockingQueue<Message> queue = new PriorityBlockingQueue<>(20);
    BlockingQueue<Message> masterQueue = new LinkedBlockingDeque<>(3);
    private MasterThread master;
    private Map<Integer, Process> vertexToProcess = new HashMap<>();

    private HashSet<NeighborObject> neighbors = new HashSet<>();
    private HashSet<Integer> children = new HashSet<>();
    private HashSet<Integer> others = new HashSet<>();  // should not include parent
    private HashSet<Integer> terminatedNeighbors = new HashSet<>();

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
        }
    }

    private void waitUntilMasterStartsNewRound() throws InterruptedException {
        while (true) {
            MasterMessage msg = ((MasterMessage) masterQueue.take());
            if (msg.getType().equals(MessageType.START_ROUND)) {
                if (msg.getMsg() > round) {
                    log.info("received " + msg);
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

    @Override
    public void run() {
        try {
            while (true) {
                waitUntilMasterStartsNewRound();
                if (selfKill) {
                    break;
                }

                //message();
                //transition();

                if (this.round == 2) {
                    sendTerminationToMaster();
                }
                else {
                    sendRoundCompletionToMaster();
                }
            }
        } catch (InterruptedException e) {

        }
    }
}
