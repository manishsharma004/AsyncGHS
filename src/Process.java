import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class Process extends Thread {
    // states
    int uid;
    int maxIdSeen;
    int round = 0;  // initially round is 0, but everything starts from round 1 (master sends this)
    int parentId = -1;
    boolean isLeader;
    boolean newInfo = true;
    boolean isReadyToTerminate = false;

    BlockingQueue<Message> queue = new LinkedBlockingDeque<>(10);
    MasterThread master;
    List<Process> neighborProcesses;
    Map<Integer, Process> vertexToProcess = new HashMap<>();

    HashSet<Integer> neighbors = new HashSet<>();
    HashSet<Integer> children = new HashSet<>();
    HashSet<Integer> others = new HashSet<>();  // should not include parent
    HashSet<Integer> terminatedNeighbors = new HashSet<>();

    // TODO: following should reset after every round, i.e. after transition() is processed
    // however, use this to calculate how many messages to expect in next round
    HashSet<Integer> receivedExploreFrom = new HashSet<>();
    HashSet<Integer> receivedNACKsFrom = new HashSet<>();
    HashSet<Integer> receivedACKsFrom = new HashSet<>();
    HashSet<Integer> receivedNullFrom = new HashSet<>();

    public void setMaster(MasterThread master) {
        this.master = master;
    }

    public Process(String name, int uid) {
        super(name);
        this.uid = uid;
        this.maxIdSeen = uid;
    }

    public int getUid() {
        return this.uid;
    }

    public void setNeighborProcesses(List<Process> neighborProcesses) {
        neighborProcesses = neighborProcesses;
        for (Process p : neighborProcesses) {
            neighbors.add(p.getUid());
            vertexToProcess.put(p.getUid(), p);
        }
    }

    @Override
    public synchronized void start() {
        super.start();
    }

    private synchronized boolean pushToQueue(Process p, Message m) {
        return p.queue.add(m);
    }

    public synchronized boolean sendMessageToNeighbor(int neighborId, Message m) {
        Process p = vertexToProcess.get(neighborId);
        return pushToQueue(p, m);
    }

    public synchronized void sendMessages(HashSet<Integer> neighbors, Message m) {
        for (Integer neighborId : neighbors) {
            sendMessageToNeighbor(neighborId, m);
        }
    }

    private synchronized boolean pushToQueue(MasterThread p, Message m) {
        return p.queue.add(m);
    }

    synchronized public boolean sendMessageToMaster(Message msg) {
        return pushToQueue(master, msg);
    }

    synchronized public void sendRoundCompletionToMaster() {
        sendMessageToMaster(new Message(uid, round, MessageType.END_ROUND));
    }

    synchronized public void sendTerminationToMaster() {
        sendMessageToMaster(new Message(uid, round, MessageType.TERMINATE));
    }

    synchronized public void waitUntilMasterStartsNewRound() throws InterruptedException {
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
            } else {
                queue.add(msg);
            }
        }
    }

    synchronized public void message() {
        // defining messages
        Message exploreMsg = new Message(uid, maxIdSeen, MessageType.EXPLORE);
        Message nackMsg = new Message(uid, MessageType.NACK);
        Message ackMsg = new Message(uid, MessageType.ACK);
        Message terminateMsg = new Message(uid, MessageType.TERMINATE);
        Message nullMsg = new Message(uid, MessageType.DUMMY);

        // find non terminated neighbors
        HashSet<Integer> nonTerminatedNeighbors = neighbors;
        nonTerminatedNeighbors.removeAll(terminatedNeighbors);

        if (isReadyToTerminate) {
            // send terminate to all neighbors
            sendMessages(neighbors, terminateMsg);
        } else {
            // first order of business, reply to EXPLORE messages
            if (parentId != -1) {
                sendMessageToNeighbor(parentId, ackMsg);
                HashSet<Integer> neighborsToSendNACK = receivedExploreFrom;
                neighborsToSendNACK.remove(parentId);
                sendMessages(neighborsToSendNACK, nackMsg);
            }

            // send NULL to those who send us NULL, basically nothing has changed between us
            sendMessages(receivedNullFrom, nullMsg);

            // share new info with neighbors
            if (newInfo) {
                if (parentId != -1) {   // we have a parent, no need to send them the same id again
                    sendMessageToNeighbor(parentId, nullMsg);
                }
                sendMessages(nonTerminatedNeighbors, exploreMsg);
            } else {
                sendMessages(nonTerminatedNeighbors, nullMsg);
            }
        }
    }

    /**
     * Termination for convergecast procedure. When a process receives notifications of completion from all its children
     * and NACK from the rest, and has a parent, and doesn't have any new info to share, it is ready to terminate.
     */
    synchronized private void checkTermination() {
        boolean allChildrenTerminated = false;
        boolean receivedNACKFromOthers = false;

        // check if a process has received notifications of completion from all its children
        HashSet<Integer> temp = children;
        temp.removeAll(terminatedNeighbors);
        if (temp.isEmpty()) {
            // all children have terminated
            allChildrenTerminated = true;
        }

        // check if a process has received NACKs from other neighbours
        // the process shouldn't expect a NACK from the parent to terminate, it may have happened in an earlier round
        temp = receivedNACKsFrom;
        temp.removeAll(others);
        if (temp.isEmpty()) {
            receivedNACKFromOthers = true;
        }

        if (!newInfo && parentId != -1 && allChildrenTerminated && receivedNACKFromOthers) {
            isReadyToTerminate = true;
        } else {
            isReadyToTerminate = false;
        }
    }

    synchronized private void checkLeader() {
        if (receivedACKsFrom.equals(neighbors)) {
            isLeader = true;
        } else {
            isLeader = false;
        }
    }

    synchronized public void transition() throws InterruptedException {
        // wait until messages from all neighborProcesses have arrived
        while (true) {
            // initial round, we expect only explore messages from our neighbours
            if (round == 1) {
                if (queue.size() >= (neighbors.size() - terminatedNeighbors.size())) {
                    break;
                }
            } else {
                // TODO: this is a hack, do a proper calculation using the states from previous round
                if (queue.size() > terminatedNeighbors.size()) {
                    break;
                }
            }
        }

        newInfo = false;    // initially I do not have any new information

        // process all messages we received
        Message msg;
        int numberOfMessagesProcessed = 0;
        while (!queue.isEmpty()) {
            msg = queue.take();
            numberOfMessagesProcessed += 1;
            switch (msg.getType()) {
                case EXPLORE:
                    if (msg.maxId > maxIdSeen) {
                        newInfo = true;
                        parentId = msg.sender;
                        maxIdSeen = msg.maxId;
                    }
                    break;
                case ACK:
                    children.add(msg.sender);
                    others.remove(msg.sender);
                    break;
                case NACK:
                    children.remove(msg.sender);
                    others.add(msg.sender);
                case DUMMY:
                    // do nothing
                    break;
                case TERMINATE:
                    terminatedNeighbors.add(msg.sender);
                    break;
                default:
                    break;
            }
        }

        // decide whether to terminate or elect self as leader
        checkTermination();
        checkLeader();
    }

    @Override
    public void run() {
        try {
            while (true) {
                waitUntilMasterStartsNewRound();
                message();
                transition();

                if (!isReadyToTerminate) {
                    sendRoundCompletionToMaster();
                } else {
                    sendTerminationToMaster();
                    throw new InterruptedException(getName() + " shutting down");
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