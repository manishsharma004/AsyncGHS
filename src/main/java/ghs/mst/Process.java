package ghs.mst;

import edu.princeton.cs.algs4.Edge;
import ghs.message.*;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * The {@code Process} represents a process in a asynchronous network that executes instructions for finding
 * the minimum spanning tree in the graph using the Asynchronous GHS algorithm.
 */
public class Process extends Thread {
    private Logger log = Logger.getLogger(this.getName());
    public Random random = new Random();
    public BlockingQueue<Message> queue = new PriorityBlockingQueue<>(30);

    // states
    private int uid;                    // my unique id
    public int leaderId;                // leader of my component
    private int parentId;               // my parent in the current component
    private Set<Integer> children;      // ids of my children in the current component
    private int level;                  // level of the component
    private int mwoeSender;             // id of the process that sent me the mwoe
    private Edge coreEdge;              // core edge, represents the id of the component
    private Edge mwoe;                  // the mwoe I have seen so far

    // for exiting and synchronization
    private int round;
    private CyclicBarrier barrier;
    private MasterThread master;
    private boolean readyToExit;
    private boolean selfKill;
    private boolean exitSent;

    // for processing  messages
    private PriorityQueue<Message> sendBuffer;
    private PriorityQueue<Message> deferQueue;
    private PriorityQueue<Connect> pendingConnects;

    // book-keeping
    private boolean connectSent;
    private boolean acceptReceivedInPhase;
    private boolean noBasicEdgesLeft;
    private List<Edge> edges;
    private PriorityQueue<Edge> basicEdges;
    private Set<Edge> branchEdges;
    private Set<Edge> rejectedEdges;
    private Set<Integer> receivedReportsFrom;

    // maps for handling asynchronous communication with neighbors
    private Map<Integer, Process> vertexToProcess = new HashMap<>();
    private Map<Integer, Integer> vertexToDelay = new HashMap<>();

    /**
     * Instantiates a new {@code Process}.
     *
     * @param name    name of the Process
     * @param uid     unique id
     * @param edges   links of the Process
     * @param barrier CyclicBarrier every other Process in the network shares
     */
    public Process(String name, int uid, List<Edge> edges, CyclicBarrier barrier) {
        super(name);

        // states
        this.uid = uid;
        this.parentId = -1;
        this.children = new HashSet<>();
        this.mwoeSender = uid;
        this.leaderId = uid;
        this.mwoe = null;
        this.edges = edges;

        // synchronization
        this.barrier = barrier;
        this.round = 0;
        this.readyToExit = false;
        this.selfKill = false;
        this.exitSent = false;

        // processing messages
        this.sendBuffer = new PriorityQueue<>(20);
        this.deferQueue = new PriorityQueue<>(10);
        this.pendingConnects = new PriorityQueue<>(10);

        // book-keeping
        this.connectSent = false;
        this.acceptReceivedInPhase = false;
        this.noBasicEdgesLeft = false;
        this.basicEdges = new PriorityQueue<>();
        this.basicEdges.addAll(edges);      // initially, all edges are basic edges
        this.branchEdges = new HashSet<>();
        this.rejectedEdges = new HashSet<>();
        this.receivedReportsFrom = new HashSet<>();

        // maps for handling asynchronous communication with neighbors
        this.vertexToProcess = new HashMap<>();
        this.vertexToDelay = new HashMap<>();
    }

    /**
     * Sets neighbor processes.
     *
     * @param neighborProcesses list of neighboring processes
     */
    public void setNeighborProcesses(List<Process> neighborProcesses) {
        for (Process p : neighborProcesses) {
            this.vertexToProcess.put(p.getUid(), p);
        }
    }

    /**
     * Sets the master thread.
     *
     * @param master MasterThread instance
     */
    public void setMaster(MasterThread master) {
        this.master = master;
    }

    /**
     * Gets the unique id of the {@code Process}
     *
     * @return unique id
     */
    public int getUid() {
        return this.uid;
    }

    /**
     * Returns the {@code Edge} that connects to a certain neighbor.
     *
     * @param neighborId neighbor id
     * @return {@code Edge}, outlink to that neighbor
     */
    private Edge getEdge(int neighborId) {
        for (Edge e : this.edges) {
            if (e.other(this.uid) == neighborId) {
                return e;
            }
        }
        return null;
    }

    /**
     * Generates a random delay in the range 1 to 20.
     *
     * <p>This helps simulate an asynchronous network where messages travel with different speeds.</p>
     *
     * @return random delay
     */
    private Integer getDelay() {
        return 1 + random.nextInt(19);
    }

    /**
     * Adds a message to the send buffer.
     *
     * <p>The send buffer holds message that must be sent in a future round.</p>
     *
     * @param m Message instance
     */
    private void addToSendBuffer(Message m) {
        this.sendBuffer.add(m);
    }

    /**
     * Computes the next round to send a message to a neighbor.
     *
     * <p>Ensures messages are added to the send buffer in the order in which they are generated. In short, if I
     * send <em>m</em> to you followed by <em>n</em>, you should process <em>m</em> first.</p>
     *
     * @param neighborId id of neighbor
     * @return round in which the message must be sent
     */
    private int getNextRound(int neighborId) {
        int prevDelay = this.vertexToDelay.getOrDefault(neighborId, 0);
        int currentDelay = getDelay();
        int finalDelay;
        if (currentDelay < prevDelay) {
            finalDelay = this.round + prevDelay + 1;
        } else {
            finalDelay = this.round + currentDelay;
        }
        this.vertexToDelay.put(neighborId, finalDelay);
        return finalDelay;
    }

    /**
     * Adds a message to another process's queue.
     *
     * @param p Process object
     * @param m Message object
     */
    private void pushToQueue(Process p, Message m) {
        p.queue.add(m);
    }

    /**
     * Adds a message to the master thread's queue
     *
     * @param p Master Thread instance
     * @param m Message instance
     */
    private void pushToQueue(MasterThread p, Exit m) {
        p.queue.add(m);
    }

    /**
     * Sends a message to master notifying of termination of the current worker process.
     */
    private void sendTerminationToMaster() {
        log.info("Sending EXIT to master");
        boolean isLeader = this.uid == this.leaderId;
        pushToQueue(this.master, new Exit(this.uid, this.coreEdge, this.branchEdges, isLeader));
    }

    /**
     * Sends a message to a neighbor.
     *
     * <p>Generates a random delay to send the message. Stores the message in the send buffer and actually sends
     * the message at a later round.</p>
     *
     * @param msg        Message to send
     * @param neighborId the id of the neighbor
     */
    private void sendMessage(Message msg, int neighborId) {
        int delay = this.round;
        // only generate a delay for a neighbor, not for myself
        if (neighborId != this.uid) {
            delay = getNextRound(neighborId);
        }
        msg.setRound(delay);
        msg.setSender(this.uid);
        msg.setReceiver(neighborId);
        addToSendBuffer(msg);
        if (msg instanceof Initiate) {
            log.debug("Sent " + msg);
        }
    }

    /**
     * Sends a message along an {@code Edge}.
     *
     * @param msg  Message
     * @param edge Edge
     */
    private void sendMessage(Message msg, Edge edge) {
        int neighborId = edge.other(this.uid);
        sendMessage(msg, neighborId);
    }


    /**
     * Sends the messages in the send buffer.
     *
     * <p>Inspects the send buffer to determine whether to send message to some other process. The actual sending of
     * the message is instantaneous.</p>
     */
    private void processSendBuffer() {
        while (!this.sendBuffer.isEmpty() && this.sendBuffer.peek().getRound() <= this.round) {
            Message m = this.sendBuffer.remove();
            if (m.getReceiver() == this.uid) {  // because we allow a process to send message to itself
                this.queue.add(m);
            } else {
                pushToQueue(this.vertexToProcess.get(m.getReceiver()), m);
            }
        }
    }

    /**
     * Processes deferred replies to {@code Test} messages.
     */
    private void processDeferQueue() {
        while (!this.deferQueue.isEmpty() && this.level >= ((Test) this.deferQueue.peek()).getLevel()) {
            Test testMsg = ((Test) this.deferQueue.remove());
            log.debug("Deferred reply to " + testMsg);
            sendTestReply(testMsg);
        }
    }

    /**
     * Test whether a connect is pending over this edge
     */
    private void processPendingConnects() {
        // process pending connects until you can no longer process them
        while (true) {
            Connect connect = this.pendingConnects.poll();
            if (connect == null) {
                break;
            }
            mergeOrAbsorb(connect);
            // if processed, pending connects won't have this connect again
            if (connect.equals(this.pendingConnects.peek())) {
                // couldn't process the connect message yet
                break;
            }
        }
    }

    /**
     * Sends a Wake Up message to self.
     *
     * <p>Wake up messages instruct the process to begin the next phase of searching for MWOEs.</p>
     */
    private void wakeUp() {
        Initiate msg = new Initiate(this.uid, this.uid, this.level, null, this.uid);
        msg.setRound(this.round);   // don't delay when sending message to self
        this.queue.add(msg);
    }

    /**
     * Broadcast an {@code Initiate} message along an {@code Edge}.
     *
     * @param edge edge along which initiate message will be broadcasted
     */
    private void broadcast(Edge edge) {
        Initiate initiateMsg = new Initiate(this.uid, -1, this.level, this.coreEdge, this.leaderId);
        int neighborId = edge.other(this.uid);
        if (neighborId != this.parentId) {
            sendMessage(initiateMsg, edge);
        }
        // children
        if (neighborId != this.parentId) {
            this.children.add(neighborId);
        }
    }

    /**
     * Broadcasts {@code Initiate} messages along the edges specified.
     *
     * <p>The process who broadcasts the {@code Initiate} message must know who its children are. However, its children
     * might be informed of their new parent, i.e. this process.</p>
     *
     * @param edges edges along which initiate message will be broadcasted
     */
    private void broadcast(Set<Edge> edges) {
        for (Edge e : edges) {
            broadcast(e);
        }
    }

    /**
     * Get the least weight basic edge. Two possible cases: I have already received a Connect over that edge, or not.
     * If I have received connect over the edge, absorb it and proceed until no basic edges are left or I find an edge
     * over which I have not received a Connect.
     */
    private void testBasicEdge() {
        processPendingConnects();
        Edge minWeightBasicEdge = this.basicEdges.poll();
        Test testMsg = new Test(this.uid, -1, this.coreEdge, this.level);
        if (minWeightBasicEdge != null) {
            log.debug("Testing " + minWeightBasicEdge);
            sendMessage(testMsg, minWeightBasicEdge);
        } else {
            this.noBasicEdgesLeft = true;
            if (foundLocalMwoe()) {
                ackReport();
            }
            log.debug("No basic edges left to test" +
                    ", acceptReceivedInPhase=" + acceptReceivedInPhase);
        }
    }

    /**
     * Check whether I received reports from all children.
     *
     * @return true if received from all children (also if no children), false otherwise
     */
    private boolean receivedReportsFromChildren() {
        if (this.children.isEmpty() && this.receivedReportsFrom.isEmpty()) {
            log.debug("I am a leaf node. Not expecting any reports.");
            return true;
        }
        return this.children.equals(this.receivedReportsFrom);
    }

    /**
     * Determines whether the process has found the MWOE.
     *
     * @return true if mwoe found.
     */
    private boolean foundLocalMwoe() {
        if (receivedReportsFromChildren() &&
                (this.acceptReceivedInPhase || this.noBasicEdgesLeft)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Update the MWOE seen so far and the path to the process that sent the MWOE.
     *
     * @param reportMsg Report message
     */
    private void updateMWOE(Report reportMsg) {
        if (children.contains(reportMsg.getSender())) {
            this.receivedReportsFrom.add(reportMsg.getSender());
        } else {
            // ignore report
            log.error("Received REPORT from non-child: " + reportMsg.getSender());
        }
        if (reportMsg.getMwoe() == null) {
            // no need to change own mwoe
            log.debug("Received no mwoe in report. My mwoe=" + this.mwoe);
        } else if (this.mwoe == null) {
            // found better mwoe from reports, i.e. children
            this.mwoe = reportMsg.getMwoe();
            this.mwoeSender = reportMsg.getSender();    // only need to store who sent me the MWOE and trace
            log.debug("Found better mwoe from " + reportMsg);
        } else {
            // both non-null
            if (reportMsg.getMwoe().compareTo(this.mwoe) < 0) {
                this.mwoe = reportMsg.getMwoe();
                this.mwoeSender = reportMsg.getSender();
                log.debug("Found better mwoe from " + reportMsg);
            }
        }
    }

    /**
     * Sends reports to parent, if non-leader or changeroot, if leader.
     *
     * <p>Called only when I have found my local mwoe and received reports from all children.</p>
     */
    private void ackReport() {
        if (this.mwoe == null) {
            this.readyToExit = true;
        }
        if (this.uid == this.leaderId) {
            log.info("Found MWOE=" + this.mwoe +
                    ", children=" + this.children +
                    ", basic edges=" + this.basicEdges);
            if (this.mwoe != null) {
                ChangeRoot cr = new ChangeRoot(this.mwoe);
                sendMessage(cr, this.mwoeSender);
            }
        } else {
            // combine information from children and report to parent
            Report reportMsg = new Report(this.mwoe);
            sendMessage(reportMsg, this.parentId);
            log.debug("Sending " + reportMsg);
        }
    }

    /**
     * Sends reply to {@code Test} message that is sent.
     *
     * <p>{@code Accept} can be sent multiple times per edge, {@code Reject} just once per edge.</p>
     *
     * @param testMsg Test message to reply to
     */
    private void sendTestReply(Test testMsg) {
        boolean inDifferentComponents = this.coreEdge == null || !this.coreEdge.equals(testMsg.getCoreEdge());
        if (inDifferentComponents && this.level >= testMsg.getLevel()) {
            Accept acceptMsg = new Accept(this.level);
            sendMessage(acceptMsg, testMsg.getSender());
        } else if (!inDifferentComponents) {
            Reject rejectMsg = new Reject();
            sendMessage(rejectMsg, testMsg.getSender());
        } else {
            log.debug("Defer replying, my level=" + this.level + ", for " + testMsg);
            this.deferQueue.add(testMsg);
        }
    }

    /**
     * Merge or absorb on receiving a {@code Connect} message.
     *
     * @param connect Connect message
     */
    private void mergeOrAbsorb(Connect connect) {
        log.debug("Received " + connect + ", connectSent=" + this.connectSent);
        if (this.level == connect.getLevel() && connect.getMwoe().equals(this.mwoe) && this.connectSent) {
            // my previous parent becomes my child now
            if (this.uid != this.leaderId && this.parentId != -1) {
                this.children.add(this.parentId);
            }
            // find new leader, larger of two ids adjacent to mwoe
            this.leaderId = this.uid > connect.getSender() ? this.uid : connect.getSender();
            // the mwoe is the core edge now, so re-classify as a branch edge
            this.branchEdges.add(this.mwoe);
            this.basicEdges.remove(this.mwoe);
            this.coreEdge = this.mwoe;
            this.level += 1;
            this.connectSent = false;   // because now, I will start new phase of searching for mwoe
            log.info("MERGE with vertex=" + connect.getSender() +
                    ", new leader=" + leaderId + ", new level=" + this.level);
            if (this.uid == this.leaderId) {
                this.parentId = -1;     // I am root
                this.children.add(connect.getSender());
                wakeUp();
            } else {
                this.parentId = connect.getSender();
            }
            log.debug("After merge, children=" + this.children + ", new parent=" + this.parentId +
                    ", basic edges=" + this.basicEdges + ", new level=" + this.level);
        } else if (this.level > connect.getLevel()) {
            // absorb this component
            this.children.add(connect.getSender());
            Edge mwoeOther = connect.getMwoe();
            // re-classify this mwoe as a branch edge
            this.branchEdges.add(mwoeOther);
            this.basicEdges.remove(mwoeOther);
            log.info("ABSORB " + connect.getSender() +
                    ", basic edges=" + this.basicEdges +
                    ", branch edges=" + this.branchEdges);
            // does not update core edge or level
            broadcast(mwoeOther);
        } else {
            if (!this.pendingConnects.contains(connect)) {
                this.pendingConnects.add(connect);
                log.info("Pending " + connect);
            }
        }
    }

    /**
     * On receiving {@code Initiate}, start searching for MWOE and broadcast {@code Initiate} along branch edges.
     *
     * @param initiateMsg Initiate message
     */
    private void handleInitiate(Initiate initiateMsg) {
        // update own state, start search for new mwoe
        this.acceptReceivedInPhase = false;
        this.mwoe = null;
        if (initiateMsg.getSender() == this.uid) {  // i.e. wake up message
            this.parentId = -1;
            this.leaderId = this.uid;
        } else {
            this.parentId = initiateMsg.getSender();
        }
        this.children.remove(parentId);
        this.receivedReportsFrom.clear();   // expecting fresh reports from all children now
        this.level = initiateMsg.getLevel();
        this.leaderId = initiateMsg.getLeader();
        // also update component id, if received from parent (not self)
        if (initiateMsg.getSender() != this.uid) {
            this.coreEdge = initiateMsg.getCoreEdge();
        }
        if (this.leaderId != this.uid) {
            log.debug("Received " + initiateMsg +
                    ", parent=" + parentId +
                    ", level=" + level +
                    ", leader=" + leaderId);
        } else {
            log.debug("Start search for next mwoe");
        }
        // broadcast initiate to all processes in component, i.e. along branch edges
        if (!this.children.isEmpty()) {
            log.debug("Starting initiate broadcast, basic edges=" + this.basicEdges +
                    ", branch edges=" + this.branchEdges);
            broadcast(this.branchEdges);
        }
        testBasicEdge();
    }

    /**
     * On receiving {@code Accept} message, I update my mwoe and report (if possible).
     *
     * @param msg Accept message
     */
    private void handleAccept(Accept msg) {
        log.debug("Received " + msg);
        Edge e = getEdge(msg.getSender());
        // add this to basic edges
        this.basicEdges.add(e);
        if (this.mwoe == null) {
            this.mwoe = e;
            this.mwoeSender = this.uid; // one of basic edges an mwoe
        } else {
            if (this.mwoe.compareTo(e) > 0) {
                this.mwoe = e;
                this.mwoeSender = this.uid;
            }
        }
        this.acceptReceivedInPhase = true;  // consider what happens if no basic edges left
        if (foundLocalMwoe()) {
            ackReport();
        }
    }

    /**
     * On receiving {@code Report} message, update mwoe and send {@code ChangeRoot} (leader) or {@code Report} to my
     * parent.
     *
     * @param reportMsg Report message
     */
    private void handleReports(Report reportMsg) {
        updateMWOE(reportMsg);
        log.debug("Received " + reportMsg + ", foundLocalMwoe=" + foundLocalMwoe());
        if (foundLocalMwoe()) {
            ackReport();
        }
    }

    /**
     * On receiving {@code ChangeRoot}, forward it along path if I am not the process adjacent to mwoe. Or, send a
     * {@code Connect} over the mwoe.
     *
     * @param crMsg Changeroot message
     */
    private void handleChangeroot(ChangeRoot crMsg) {
        log.debug("Received " + crMsg);
        this.mwoe = crMsg.getMwoe();    // update the mwoe of my component
        this.basicEdges.remove(this.mwoe); // mwoe becomes branch edge
        int u = this.mwoe.either();
        int v = this.mwoe.other(u);
        if (u == this.uid || v == this.uid) {   // I am the process adjacent to mwoe
            Connect connect = new Connect(this.level, this.mwoe);
            sendMessage(connect, this.mwoe);    // send connect over this edge
            this.connectSent = true;
        } else {
            // forward changeroot along the path
            ChangeRoot forwardMsg = new ChangeRoot(this.mwoe);
            sendMessage(forwardMsg, this.mwoeSender);
        }
    }

    /**
     * If a {@code Reject} is received, test the next basic edge.
     *
     * @param msg Reject message
     */
    private void handleReject(Message msg) {
        Edge e = getEdge(msg.getSender());  // edge along which reject was sent
        this.rejectedEdges.add(e);
        this.basicEdges.remove(e); // if reject sent, no longer a basic edge (don't test again)
        log.debug("Received " + msg +
                ", basic edges=" + this.basicEdges +
                ", rejected edges=" + this.rejectedEdges);
        if (foundLocalMwoe()) {
            ackReport();
        } else {
            // test next basic edge
            testBasicEdge();
        }
    }

    /**
     * Processes the messages I receive.
     *
     * @throws InterruptedException
     */
    private void handleMessages() throws InterruptedException {
        Message msg;
        while (!this.queue.isEmpty()) {
            msg = this.queue.take();
            // sometimes multiple initiate messages are sent because of wake up, merge and absorb operations
            if (msg instanceof Initiate) {
                Initiate initiateMsg = ((Initiate) msg);
                handleInitiate(initiateMsg);
            } else if (msg instanceof Test) {
                Test testMsg = ((Test) msg);
                sendTestReply(testMsg);
            } else if (msg instanceof Accept) {
                Accept acceptMsg = ((Accept) msg);
                handleAccept(acceptMsg);
            } else if (msg instanceof Reject) {
                Reject rejectMsg = ((Reject) msg);
                handleReject(msg);
            } else if (msg instanceof Report) {
                Report reportMsg = ((Report) msg);
                handleReports(reportMsg);
            } else if (msg instanceof ChangeRoot) { // note current leader can't receive a changeroot message
                // check if my id is adjacent to mwoe
                ChangeRoot crMsg = ((ChangeRoot) msg);
                handleChangeroot(crMsg);
            } else if (msg instanceof Connect) {
                Connect connectMsg = ((Connect) msg);
                mergeOrAbsorb(connectMsg);
            } else if (msg instanceof Exit) {
                this.selfKill = true;
                log.info("Received KILL from MASTER");
            }
        }
    }

    /**
     * This defines what I do.
     *
     * @throws InterruptedException
     * @throws BrokenBarrierException
     */
    private void executeTasks() throws InterruptedException, BrokenBarrierException {
        processDeferQueue();
        processSendBuffer();
        this.barrier.await();
        handleMessages();
        this.barrier.await();
    }

    @Override
    public void run() {
        try {
            wakeUp();
            while (true) {
                executeTasks();

                if (this.readyToExit && this.sendBuffer.isEmpty() && !this.exitSent) {
                    log.info("Branch edges=" + branchEdges);
                    sendTerminationToMaster();
                    this.exitSent = true;
                }

                if (this.selfKill) {
                    break;
                }

                this.round++;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (BrokenBarrierException e) {
            e.printStackTrace();
        }
    }
}
