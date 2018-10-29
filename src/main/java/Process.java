import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.LinkedBlockingDeque;

public class Process extends Thread {
    private Logger log = Logger.getLogger(this.getName());

    // states
    private int uid;
    private int maxIdSeen;
    private int round = 0;  // initially round is 0, but everything starts from round 1 (master sends this)
    private int parentId = -1;
    private boolean isLeader;
    private boolean isReadyToTerminate = false;
    private boolean selfKill = false;

    private CyclicBarrier barrier;

    // each neighbor will send at most two messages
    BlockingQueue<Message> queue = new LinkedBlockingDeque<>(20);
    private MasterThread master;
    private Map<Integer, Process> vertexToProcess = new HashMap<>();

    private HashSet<Integer> neighbors = new HashSet<>();
    private HashSet<Integer> children = new HashSet<>();
    private HashSet<Integer> others = new HashSet<>();  // should not include parent
    private HashSet<Integer> terminatedNeighbors = new HashSet<>();

    // following should reset after every round, i.e. after transition() is processed
    private HashSet<Integer> receivedExploreFrom = new HashSet<>();
    private HashSet<Integer> receivedNACKsFrom = new HashSet<>();
    private HashSet<Integer> receivedACKsFrom = new HashSet<>();

    public Process(String name, int uid, CyclicBarrier barrier) {
        super(name);
        this.uid = uid;
        this.maxIdSeen = uid;
        this.barrier = barrier;
    }

    public void setMaster(MasterThread master) {
        this.master = master;
    }

    public int getUid() {
        return this.uid;
    }

    public void setNeighborProcesses(List<Process> neighborProcesses) {
        for (Process p : neighborProcesses) {
            neighbors.add(p.getUid());
            vertexToProcess.put(p.getUid(), p);
        }
    }

    @Override
    public void start() {
        super.start();
    }

    private void pushToQueue(Process p, Message m) {
        p.queue.add(m);
    }

    private void sendMessageToNeighbor(int neighborId, Message m) {
        Process p = vertexToProcess.get(neighborId);
        pushToQueue(p, m);
    }

    private void sendMessages(HashSet<Integer> neighbors, Message m) {
        for (Integer neighborId : neighbors) {
            sendMessageToNeighbor(neighborId, m);
        }
    }

    private void pushToQueue(MasterThread p, Message m) {
        p.queue.add(m);
    }

    private void sendMessageToMaster(Message msg) {
        pushToQueue(master, msg);
    }

    private void sendRoundCompletionToMaster() {
        sendMessageToMaster(new Message(uid, round, MessageType.END_ROUND));
    }

    private void sendTerminationToMaster() {
        sendMessageToMaster(new Message(uid, parentId, MessageType.TERMINATE));
    }

    private void sendTerminationToProcess() {
        sendMessages(neighbors, new Message(getUid(), MessageType.TERMINATE));
    }

    private void waitUntilMasterStartsNewRound() throws InterruptedException {
        while (true) {
            Message msg = queue.take();
            if (msg.getType().equals(MessageType.START_ROUND)) {
                if (msg.maxId > round) {
                    round = msg.maxId;
                } else {
                    // This should never happen
                    throw new InterruptedException("Received round < current round");
                }
                return;
            } else if (msg.getType().equals(MessageType.KILL)) {
                selfKill = true;
                break;
            } else {
                queue.add(msg);
            }
        }
    }

    private void message() {
        // defining messages
        Message exploreMsg = new Message(uid, maxIdSeen, MessageType.EXPLORE);
        sendMessages(neighbors, exploreMsg);
    }

    /**
     * Termination for convergecast procedure. When a process receives notifications of completion from all its children
     * and NACK from the rest, and has a parent, and doesn't have any new info to share, it is ready to terminate.
     */
    private void checkTermination() {
        boolean allChildrenTerminated = false;
        boolean receivedNACKFromOthers = false;

        // check if a process has received notifications of completion from all its children
        HashSet<Integer> temp = ((HashSet<Integer>) children.clone());
        temp.removeAll(terminatedNeighbors);
        if (temp.isEmpty()) {
            // all children have terminated
            allChildrenTerminated = true;
        }

        // check if a process has received NACKs from other neighbours
        // the process shouldn't expect a NACK from the parent to terminate, it may have happened in an earlier round
        temp = ((HashSet<Integer>) receivedNACKsFrom.clone());
        temp.removeAll(others);
        if (temp.isEmpty() && !others.isEmpty()) {
            receivedNACKFromOthers = true;
        }

        if (parentId != -1 && allChildrenTerminated && receivedNACKFromOthers) {
            isReadyToTerminate = true;
        } else isReadyToTerminate = parentId == -1 && allChildrenTerminated && isLeader;

    }

    private void checkLeader() {
        isLeader = receivedACKsFrom.equals(neighbors);
    }

    private void processMessage(Message msg) {
        switch (msg.getType()) {
            case EXPLORE:
                if (msg.maxId > maxIdSeen) {
                    parentId = msg.sender;
                    maxIdSeen = msg.maxId;
                }
                receivedExploreFrom.add(msg.sender);
                break;
            case ACK:
                children.add(msg.sender);
                others.remove(msg.sender);
                receivedACKsFrom.add(msg.sender);
                break;
            case NACK:
                children.remove(msg.sender);
                others.add(msg.sender);
                receivedNACKsFrom.add(msg.sender);
                isLeader = false;
                break;
            case TERMINATE:
                terminatedNeighbors.add(msg.sender);
                break;
            default:
                break;
        }
    }

    private void handleMessages() throws InterruptedException {
        Message msg;
        while (!queue.isEmpty()) {
            msg = queue.take();
            processMessage(msg);
        }
    }

    private void transition() throws InterruptedException, BrokenBarrierException {
        barrier.await();    // wait until all threads have sent explore messages

        // process all explore messages we received
        handleMessages();

        HashSet<Integer> sentNACK = new HashSet<Integer>();
        // send ack, nacks - ack to parent, nack to rest
        for (int id : neighbors) {
            if (id == parentId) {
                sendMessageToNeighbor(id, new Message(this.uid, MessageType.ACK));
            } else {
                sendMessageToNeighbor(id, new Message(this.uid, MessageType.NACK));
                sentNACK.add(id);
            }
        }

        log.debug("Received EXPLORE from " + receivedExploreFrom + ", sent ACK to " + parentId
                + ", sent NACK to " + sentNACK);

        barrier.await();
        handleMessages();
        log.debug("Received ACK from " + receivedACKsFrom + ", NACK from " + receivedNACKsFrom);

        // decide whether to terminate or elect self as leader
        checkTermination();
        checkLeader();
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

                // clear
                receivedExploreFrom.clear();
                receivedNACKsFrom.clear();
                receivedACKsFrom.clear();

                if (!isReadyToTerminate) {
                    sendRoundCompletionToMaster();
                } else {
                    log.info("Ready to TERMINATE");
                    sendTerminationToProcess();
                    sendTerminationToMaster();
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
                '}';
    }
}