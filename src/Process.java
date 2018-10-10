import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
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

    CyclicBarrier barrier;

    // TODO: max size of deque should be twice that of the number of neighbors
    // because each neighbor will send at most two messages
    BlockingQueue<Message> queue = new LinkedBlockingDeque<>(20);
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

    public Process(String name, int uid, CyclicBarrier barrier) {
        super(name);
        this.uid = uid;
        this.maxIdSeen = uid;
        this.barrier = barrier;
    }

    public int getUid() {
        return this.uid;
    }

    public void setNeighborProcesses(List<Process> neighborProcesses) {
        this.neighborProcesses = neighborProcesses;
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

        sendMessages(neighbors, exploreMsg);
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
        // TODO: Does this handle the first round?
        if (temp.isEmpty()) {
            // all children have terminated
            allChildrenTerminated = true;
        }

        // check if a process has received NACKs from other neighbours
        // the process shouldn't expect a NACK from the parent to terminate, it may have happened in an earlier round
        temp = receivedNACKsFrom;
        temp.removeAll(others);
        if (temp.isEmpty() && !others.isEmpty()) {
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

    public void processMessage(Message msg) {
        switch (msg.getType()) {
            case EXPLORE:
                if (msg.maxId > maxIdSeen) {
                    newInfo = true;
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
                break;
            case DUMMY:
                receivedNullFrom.add(msg.sender);
                break;
            case TERMINATE:
                terminatedNeighbors.add(msg.sender);
                break;
            default:
                break;
        }
    }

    synchronized public void handleMessages() throws InterruptedException {
        Message msg;
        while (!queue.isEmpty()) {
            msg = queue.take();
            processMessage(msg);
        }
    }

    synchronized public void transition() throws InterruptedException, BrokenBarrierException {
        newInfo = false;    // initially I do not have any new information

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

        System.out.println(this.uid + " got EXPLORE from " + receivedExploreFrom + ", sent ACK to " + parentId
                + ", sent NACK to " + sentNACK);

        barrier.await();

        handleMessages();

        System.out.println(this.uid + " received ACK from " + receivedACKsFrom + ", NACK from " + receivedNACKsFrom);
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

                // clear
                receivedExploreFrom.clear();
                receivedNACKsFrom.clear();
                receivedACKsFrom.clear();
                receivedNullFrom.clear();

                if (!isReadyToTerminate) {
                    sendRoundCompletionToMaster();
                } else {
                    System.out.println(this.uid + " is ready to TERMINATE");
                    // TODO: handle termination in master thread
                    // this is why the program is stuck
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