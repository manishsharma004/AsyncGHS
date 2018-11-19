package asyncGHS;


import edu.princeton.cs.algs4.Edge;
import floodmax.MessageType;
import ghs.message.*;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.concurrent.*;

public class Process extends Thread {

    public Random random = new Random();
    public BlockingQueue<Message> queue = new PriorityBlockingQueue<>(30, Message::compareTo);
    public int leaderId;  // leader of the current component
    BlockingQueue<Message> masterQueue = new LinkedBlockingDeque<>(10);
    private Logger log = Logger.getLogger(this.getName());
    // states
    private int uid;
    private int round = 0;  // initially round is 0, but everything starts from round 1 (master sends this)
    private boolean selfKill = false;
    // for async broadcast convergecast
    private int parentId = -1;  // my parent in the current spanning tree component
    private int level = 0;  // level of current component I am in
    private int mwoeSender;    // who sent the mwoe
    private Edge coreEdge = null;
    private Edge mwoe = null;   // the mwoe I have seen so far
    private CyclicBarrier barrier;
    private BlockingQueue<Message> sendBuffer = new PriorityBlockingQueue<>(30, Message::compareTo);
    private BlockingQueue<Message> deferQueue = new LinkedBlockingQueue<>(10);
    private MasterThread master;

    private List<Edge> edges;

    // book-keeping
    private Set<Integer> children = new HashSet<>();    // initially no one
    private Set<Integer> receivedReportsFrom = new HashSet<>();
    private SortedSet<Edge> basicEdges = new TreeSet<>();
    private PriorityQueue<Edge> edgesToTest = new PriorityQueue<>();  // initially, all basic edges
    private SortedSet<Edge> branchEdges = new TreeSet<>();
    private SortedSet<Edge> rejectedEdges = new TreeSet<>();
    private Queue<Connect> pendingConnects = new LinkedList<>();

    private Map<Integer, Process> vertexToProcess = new HashMap<>();
    private Map<Integer, Integer> vertexToDelay = new HashMap<>();

    /**
     * Instantiates a new Process.
     *
     * @param name    name of the process
     * @param uid     unique id
     * @param edges   links of the process
     * @param barrier CyclicBarrier every other process in the network shares
     */
    public Process(String name, int uid, List<Edge> edges, CyclicBarrier barrier) {
        super(name);
        this.uid = uid;
        this.mwoeSender = uid;
        this.leaderId = uid;
        this.barrier = barrier;
        this.edges = edges;
        this.basicEdges.addAll(edges);
        this.edgesToTest.addAll(this.basicEdges);
    }

    public void setNeighborProcesses(List<Process> neighborProcesses) {
        for (Process p : neighborProcesses) {
            vertexToProcess.put(p.getUid(), p);
        }
    }

    public void setMaster(MasterThread master) {
        this.master = master;
    }

    public int getUid() {
        return this.uid;
    }

    /**
     * Returns the Edge that connects to a certain neighbor.
     *
     * @param neighborId neighbor id
     * @return Edge, outlink to that neighbor
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
     * Generates a random delay in the range 1 to 20. This helps simulate an asynchronous network where messages travel
     * with different speeds.
     *
     * @return random delay
     */
    private Integer getDelay() {
        return 1 + random.nextInt(19);
    }

    /**
     * Adds a message to the send buffer. The send buffer holds message that must be sent in a future round.
     *
     * @param m message
     */
    private void addToSendBuffer(Message m) {
        sendBuffer.add(m);
    }

    /**
     * Ensures messages are added to the send buffer in the order in which they are generated. In short, I if send
     * <em>m</em> to you followed by <em>n</em>, you should process <em>m</em> first.
     *
     * @param neighborId id of neighbor
     * @return round in which the message must be sent
     */
    private int getRound(int neighborId) {
        int prevDelay = vertexToDelay.getOrDefault(neighborId, 0);
        int currentDelay = getDelay();
        int finalDelay = 0;
        if (currentDelay < prevDelay) {
            finalDelay = this.round + prevDelay + 1;
        } else {
            finalDelay = this.round + currentDelay;
        }
        vertexToDelay.put(neighborId, finalDelay);
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
     * @param p Master Process object
     * @param m Message object
     */
    private void pushToQueue(MasterThread p, MasterMessage m) {
        p.queue.add(m);
    }

    /**
     * Sends a message to master notifying of termination of the current worker thread/process.
     */
    private void sendTerminationToMaster() {
        pushToQueue(master, new MasterMessage(uid, parentId, MessageType.TERMINATE));
    }

    /**
     * Sends a message to a neighbor.
     *
     * @param msg        the message to send
     * @param neighborId the id of the neighbor
     */
    private void sendMessage(Message msg, int neighborId) {
        int delay = this.round;
        if (neighborId != this.uid) {
            delay = getRound(neighborId);
        }
        msg.setRound(delay);
        msg.setSender(this.uid);
        msg.setReceiver(neighborId);
        addToSendBuffer(msg);
    }

    /**
     * Sends a message along an outlink.
     *
     * @param msg  message
     * @param edge outlink
     */
    private void sendMessage(Message msg, Edge edge) {
        int neighborId = edge.other(this.uid);
        sendMessage(msg, neighborId);
    }

    /**
     * Send a message along certain outlinks. Note that is allows message to have random receiver ids. It sets the right
     * received id before sending a message.
     *
     * @param msg   message
     * @param edges set of outlinks
     */
    private void sendMessages(Message msg, Set<Edge> edges) {
        for (Edge e : edges) {
            int neighborId = e.other(this.uid);
            // don't send message to your parent
            if (neighborId != parentId) {
                msg.setReceiver(neighborId);    // message intended for this neighbor
                sendMessage(msg, neighborId);
            }
        }
    }

    /**
     * Inspects the send buffer to determine whether to send message to some other process. Recall that the actually
     * message sending is instantaneous.
     *
     * @throws InterruptedException
     */
    private void processSendBuffer() throws InterruptedException {
        while (!sendBuffer.isEmpty() && sendBuffer.peek().getRound() <= round) {
            Message m = sendBuffer.take();
            if (m.getReceiver() == this.uid) {  // because we allow a process to send message to itself
                queue.add(m);
            } else {
                pushToQueue(vertexToProcess.get(m.getReceiver()), m);
            }
        }
    }

    /**
     * Processes deferred replies to Test messages.
     *
     * @throws InterruptedException
     */
    private void processDeferQueue() throws InterruptedException {
        while (!deferQueue.isEmpty() && this.level >= ((Test) deferQueue.peek()).getLevel()) {
            Test testMsg = ((Test) deferQueue.take());
            // reply now
            log.debug("Deferred reply to test message " + testMsg);
            sendTestReply(testMsg);
        }
    }

    private void processPendingConnects() {
        while (!pendingConnects.isEmpty() && this.level > ((Connect) pendingConnects.peek()).getLevel()) {
            Connect connect = ((Connect) pendingConnects.remove());
            // process this
            log.debug("Processing pending " + connect);
            mergeOrAbsorb(connect);
        }
    }

    /**
     * Initially, send an initiate message to self, in order to begin searching for MWOEs.
     */
    private void wakeUp() {
        Initiate msg = new Initiate(this.uid, this.uid, this.level, null, this.uid);
        msg.setRound(this.round);   // don't delay when sending message to self
        log.debug("Sent " + msg + " to start search for MWOEs.");
        queue.add(msg);
    }

    /**
     * Broadcasts initiate messages along the edges specified. The process who broadcasts the initiate message must
     * know who its children are. Why? However, its children might be informed of their new parent, i.e. this process.
     *
     * @param edges edges along which initiate message will be broadcasted
     */
    private void broadcast(Set<Edge> edges) {
        // broadcast along branch edges
        Initiate initiateMsg = new Initiate(this.uid, -1, this.level, this.coreEdge, this.leaderId);
        log.debug("Broadcasting INITIATE along edges: " + edges);
        sendMessages(initiateMsg, edges);
        // children
        for (Edge e : edges) {
            int neighborId = e.other(this.uid);
            if (neighborId != parentId) {
                this.children.add(neighborId);
            }
        }
    }

    /**
     * Broadcast an initiate message along a single edge. Useful in case of absorbing a smaller component.
     *
     * @param edge edge along which initiate message will be broadcasted
     */
    private void broadcast(Edge edge) {
        Initiate initiateMsg = new Initiate(this.uid, -1, this.level, this.coreEdge, this.leaderId);
        sendMessage(initiateMsg, edge);
        // children
        int neighborId = edge.other(this.uid);
        if (neighborId != parentId) {
            this.children.add(neighborId);
        }
    }

    /**
     * Tests the least weight basic edge.
     */
    private void testBasicEdge() {
        log.debug("Edges to test: " + this.edgesToTest);
        Edge minWeightBasicEdge = this.edgesToTest.poll();
        Test testMsg = new Test(this.uid, -1, this.coreEdge, this.level);
        if (minWeightBasicEdge != null) {
            sendMessage(testMsg, minWeightBasicEdge);
        }
    }

    /**
     * Check whether I received reports from all children.
     *
     * @return true if received from all children (also if no children), false otherwise
     */
    private boolean receivedReportsFromChildren() {
        return this.children.equals(this.receivedReportsFrom);
    }

    /**
     * Update the MWOE seen so far and the path to the process that sent the MWOE.
     *
     * @param reportMsg
     */
    private void updateMWOE(Report reportMsg) {
        if (this.mwoe == null || reportMsg.getMwoe().compareTo(this.mwoe) < 0) { // found better mwoe
            this.mwoe = reportMsg.getMwoe();
            this.mwoeSender = reportMsg.getSender();    // only need to store who sent me the MWOE and trace
        }
        this.receivedReportsFrom.add(reportMsg.getSender());
    }

    /**
     * Sends reports to parent, if non-leader or changeroot, if leader. Called only when I receive reports from all
     * children.
     */
    private void ackReport() {
        if (this.uid == this.leaderId) {
            log.info("Found MWOE=" + this.mwoe);
            ChangeRoot cr = new ChangeRoot(this.mwoe);
            sendMessage(cr, this.mwoeSender);
        } else {
            // combine information from children and report to parent
            Report reportMsg = new Report(this.mwoe);
            sendMessage(reportMsg, parentId);
        }
    }

    /**
     * Sends reply to test message that is sent. Accept can be sent multiple times, reject just once per edge.
     *
     * @param testMsg Test message to reply to
     */
    private void sendTestReply(Test testMsg) {
        boolean inDifferentComponents = this.coreEdge == null || !this.coreEdge.equals(testMsg.getCoreEdge());
        if (inDifferentComponents && this.level >= testMsg.getLevel()) {
            Accept acceptMsg = new Accept(this.level);
            log.debug("My component=" + this.coreEdge +
                    ", send accept for " + testMsg);
            sendMessage(acceptMsg, testMsg.getSender());
        } else if (!inDifferentComponents) {
            Reject rejectMsg = new Reject();
            // TODO: debug why reject message has never been sent
            log.debug("Sent reject for " + testMsg);
            sendMessage(rejectMsg, testMsg.getSender());
        } else {
            log.debug("Defer replying, my level=" + this.level + ", for " + testMsg);
            this.deferQueue.add(testMsg);
        }
    }

    /**
     * Merge or absorb on receiving a connect message.
     *
     * @param connect Connect message
     */
    private void mergeOrAbsorb(Connect connect) {
        if (this.level == connect.getLevel() && connect.getMwoe().equals(this.mwoe)) {
            // find new leader, larger of two ids adjacent to mwoe
            this.leaderId = this.uid > connect.getSender() ? this.uid : connect.getSender();
            this.branchEdges.add(this.mwoe);
            this.basicEdges.remove(this.mwoe);
            this.coreEdge = this.mwoe;
            this.level += 1;
            this.edgesToTest.clear();
            this.edgesToTest.addAll(this.basicEdges);
            log.info("Merge with component " + vertexToProcess.get(connect.getSender()).leaderId +
                    ", vertex=" + connect.getSender() +
                    ", new leader=" + leaderId + ", new level=" + this.level);
            if (this.uid == this.leaderId) {
                wakeUp();
            }
        } else if (this.level > connect.getLevel()) {
            log.info("Absorb " + connect.getSender());
            // absorb this component
            Edge mwoeOther = connect.getMwoe();
            this.branchEdges.add(mwoeOther);
            this.basicEdges.remove(mwoeOther);
            // does not update core edge or level
            broadcast(mwoeOther);
        } else {
            // TODO: how to deal with these?
            // idea: on level change, process pending queue
            if (!pendingConnects.contains(connect)) {
                pendingConnects.add(connect);
                log.info("Added to pending queue: " + connect);
            }
        }
    }

    /**
     * On receiving initiate, start searching for MWOE and broadcast initiate along branch edges.
     *
     * @param initiateMsg
     */
    private void handleInitiate(Initiate initiateMsg) {
        this.mwoe = null;   // initiate means finding new mwoe, so we reset
        this.parentId = initiateMsg.getSender() == this.uid ? -1 : initiateMsg.getSender(); // handling wake up messages
        this.children.remove(parentId);
        this.level = initiateMsg.getLevel();
        this.leaderId = initiateMsg.getLeader();
        // also update component id, if received from parent (not self)
        if (initiateMsg.getSender() != this.uid) {
            this.coreEdge = initiateMsg.getCoreEdge();
        }
        log.debug("Received " + initiateMsg +
                ", parent=" + parentId +
                ", level=" + level +
                ", leader=" + leaderId);
        // broadcast initiate to all processes in component, i.e. along branch edges
        if (!this.branchEdges.isEmpty()) {
            broadcast(this.branchEdges);
        }
        testBasicEdge();
        log.debug("Children=" + children + ", parent=" + parentId);
    }

    /**
     * When I receive an accept message, I update my mwoe and send changeroot (if leader) or reports (if non leader).
     *
     * @param msg Accept message
     */
    private void handleAccept(Accept msg) {
        Edge e = getEdge(msg.getSender());
        if (this.mwoe == null) {
            this.mwoe = e;
        } else {
            if (this.mwoe.compareTo(e) > 0) {
                this.mwoe = e;
            }
        }
        this.mwoeSender = this.uid;
        if (this.children.isEmpty()) {
            ackReport();
        }
    }

    /**
     * On receiving report message, update mwoe and send changeroot (leader) or reports (non leader).
     *
     * @param reportMsg Report message
     */
    private void handleReports(Report reportMsg) {
        updateMWOE(reportMsg);
        if (receivedReportsFromChildren()) {
            ackReport();
        }
    }

    /**
     * On receiving changeroot.
     *
     * @param crMsg Changeroot message
     */
    private void handleChangeroot(ChangeRoot crMsg) {
        this.mwoe = crMsg.getMwoe();    // update the mwoe of my component
        this.branchEdges.add(this.mwoe);
        this.basicEdges.remove(this.mwoe);
        int u = this.mwoe.either();
        int v = this.mwoe.other(u);
        if (u == this.uid || v == this.uid) {   // I am the process adjacent to mwoe
            Connect connect = new Connect(this.level, this.mwoe);
            sendMessage(connect, this.mwoe);    // send connect over this edge
        } else {
            // forward changeroot along the path
            ChangeRoot forwardMsg = new ChangeRoot(this.mwoe);
            sendMessage(forwardMsg, this.mwoeSender);
        }
    }

    private void handleMessages() throws InterruptedException {
        Message msg;
        while (!queue.isEmpty()) {
            msg = queue.take();
            if (msg.getRound() == null) {
                throw new IllegalStateException("Message must have a round.");
            }
            // TODO: keep track of messages sent in a particular level
            // sometimes multiple initiate messages are sent because of wake up, merge and absorb operations
            if (msg instanceof Initiate) {
                Initiate initiateMsg = ((Initiate) msg);
                handleInitiate(initiateMsg);
            } else if (msg instanceof Test) {
                Test testMsg = ((Test) msg);
//                log.debug("Received " + testMsg);
                sendTestReply(testMsg);
            } else if (msg instanceof Accept) {
                Accept acceptMsg = ((Accept) msg);
//                log.debug("Received " + acceptMsg);
                handleAccept(acceptMsg);
            } else if (msg instanceof Reject) {
                Edge e = getEdge(msg.getSender());  // edge along which reject was sent
                log.debug("Received " + ((Reject) msg));
                this.rejectedEdges.add(e);
                this.basicEdges.remove(e);  // no longer a basic edge if reject is sent
            } else if (msg instanceof Report) {
                Report reportMsg = ((Report) msg);
                log.debug("Received " + reportMsg);
                handleReports(reportMsg);
            } else if (msg instanceof ChangeRoot) { // note current leader can't receive a changeroot message
                // check if my id is adjacent to mwoe
                ChangeRoot crMsg = ((ChangeRoot) msg);
                log.debug("Received " + crMsg);
                handleChangeroot(crMsg);
            } else if (msg instanceof Connect) {
                Connect connectMsg = ((Connect) msg);
                mergeOrAbsorb(connectMsg);
            }
        }
    }

    private void transition() throws InterruptedException, BrokenBarrierException {
        // process defer queue, pending connects and send buffer
        processDeferQueue();
        processSendBuffer();
        processPendingConnects();
        barrier.await();
        handleMessages();
        barrier.await();
    }

    @Override
    public void run() {
        try {
            wakeUp();
            while (true) {
                // TODO: how to initialize selfKill
                if (selfKill) {
                    break;
                }
                transition();

                // TODO: terminate correctly, this block is incorrect
                if (this.basicEdges.isEmpty()) {
                    log.info("No basic edges found! Branch edges: " + branchEdges);
                    sendTerminationToMaster();
                    selfKill = true;
                }

                this.round++;

                // TODO: this indicates error, remove this when termination is figured out
                if (this.round == 100000) {
                    log.error("ROUND = 100000");
                    sendTerminationToMaster();
                    break;
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (BrokenBarrierException e) {
            e.printStackTrace();
        }
    }
}
