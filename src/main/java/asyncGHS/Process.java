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
     * Inspects the send buffer to determine whether to send message to some other process. Recall that the actually
     * message sending is instantaneous.
     *
     * @throws InterruptedException
     */
    private void processSendBuffer() throws InterruptedException {
        // also process defer queue
        while (!deferQueue.isEmpty()) {
            Test msg = ((Test) deferQueue.peek());
            if (this.level >= msg.getLevel()) {
                sendTestReply(msg);
            }
        }
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

    /**
     * Sends a message to a neighbor.
     *
     * @param msg        the message to send
     * @param neighborId the id of the neighbor
     */
    private void sendMessage(Message msg, int neighborId) {
        int delay = getRound(neighborId);
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
     * Initially, send an <code>initiate</code> message to self, in order to begin searching for MWOEs.
     */
    private void wakeUp() {
        Initiate msg = new Initiate(this.uid, this.uid, this.level, null, this.uid);
        msg.setRound(this.round);    // initially
        log.debug("Sent WAKE UP " + msg);
        queue.add(msg);
    }

    private void broadcast() {
        // broadcast along branch edges
        Initiate initiateMsg = new Initiate(this.uid, -1, this.level, this.coreEdge, this.leaderId);
        if (!this.branchEdges.isEmpty()) {
//            log.debug("Broadcasting INITIATE along branch edges: " + this.branchEdges);
            sendMessages(initiateMsg, this.branchEdges);
            // children
            for (Edge e : this.branchEdges) {
                int neighborId = e.other(this.uid);
                if (neighborId != parentId) {
                    this.children.add(neighborId);
                }
            }
//            log.debug("Children=" + children + ", parent=" + parentId);
        }
    }

    private void testBasicEdge() {
        // send test message along minimum weight basic edge
//        log.debug("Edges to test: " + this.edgesToTest);
        Edge minWeightBasicEdge = this.edgesToTest.poll();
        Test testMsg = new Test(this.uid, -1, this.coreEdge, this.level);
        sendMessage(testMsg, minWeightBasicEdge);
    }

    private boolean receivedReportsFromChildren() {
        return this.children.equals(this.receivedReportsFrom);
    }

    private void updateMWOE(Report reportMsg) {
        if (this.mwoe == null || reportMsg.getMwoe().compareTo(this.mwoe) < 0) { // found better mwoe
            this.mwoe = reportMsg.getMwoe();
            this.mwoeSender = reportMsg.getSender();
        }
        this.receivedReportsFrom.add(reportMsg.getSender());
    }

    private void handleReports() {
        if (this.uid == this.leaderId) {
            // found mwoe
            log.info("Found MWOE=" + this.mwoe);
            ChangeRoot cr = new ChangeRoot(this.mwoe);
            sendMessage(cr, this.mwoeSender);
        } else {
            // combine information from children and report to parent
            Report reportMsg = new Report(this.mwoe);
            sendMessage(reportMsg, parentId);
        }
    }

    private void sendTestReply(Test testMsg) {
        // TODO: below check is tricky, initially core edge is null
        boolean inDifferentComponents = this.coreEdge == null || !this.coreEdge.equals(testMsg.getCoreEdge());
        if (inDifferentComponents && this.level >= testMsg.getLevel()) {
            Accept acceptMsg = new Accept(this.level);
            sendMessage(acceptMsg, testMsg.getSender());
        } else if (!inDifferentComponents) {
            Reject rejectMsg = new Reject();
            sendMessage(rejectMsg, testMsg.getSender());
        } else {
            log.debug("Defer, my level=" + this.level + ", for " + testMsg);
        }
    }

    private void mergeOrAbsorb(Connect connect) {
        if (this.level == connect.getLevel() && connect.getMwoe().equals(this.mwoe)) {
            // find new leader, larger of two ids adjacent to mwoe
            this.leaderId = this.uid > connect.getSender() ? this.uid : connect.getSender();
            this.branchEdges.add(this.mwoe);
            this.basicEdges.remove(this.mwoe);
            this.coreEdge = this.mwoe;
            this.level += 1;
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
            broadcast();
        } else {
            // TODO: how to deal with these?
            pendingConnects.add(connect);
            log.error("Added to pending queue: " + connect);
        }
    }

    private void handleMessages() throws InterruptedException {
        Message msg;
        while (!queue.isEmpty()) {
            msg = queue.take();
            if (msg.getRound() == null) {
                throw new IllegalStateException("Message must have a round.");
            }
            if (msg instanceof Initiate) {
                Initiate initiateMsg = ((Initiate) msg);
                this.mwoe = null;   // initiate means finding new mwoe, so we reset
                // if received initiate from self don't update parent
                if (parentId == -1 && initiateMsg.getSender() != this.uid) {   // if parent not set
                    parentId = initiateMsg.getSender();
                    level = initiateMsg.getLevel();
                    leaderId = initiateMsg.getLeader();
                }
                log.debug("Received " + initiateMsg +
                        ", parent=" + parentId +
                        ", level=" + level +
                        ", leader=" + leaderId);
                // broadcast initiate to all processes in component, i.e. along branch edges
                this.edgesToTest.clear();
                this.edgesToTest.addAll(this.basicEdges);
                broadcast();
                testBasicEdge();
//                log.debug("Children=" + children + ", parent=" + parentId);
            } else if (msg instanceof Test) {
                Test testMsg = ((Test) msg);
                sendTestReply(testMsg);
            } else if (msg instanceof Accept) {
                Accept acceptMsg = ((Accept) msg);
                Edge e = getEdge(msg.getSender());
//                log.debug("Received " + acceptMsg);
                if (this.mwoe == null) {
                    this.mwoe = e;
                } else {
                    if (this.mwoe.compareTo(e) > 0) {
                        this.mwoe = e;
                    }
                }
                if (this.children.isEmpty()) {
                    // update mwoe
                    this.mwoe = getEdge(msg.getSender());
                    this.mwoeSender = this.uid;
                    handleReports();
                }
            } else if (msg instanceof Reject) {
                Edge e = getEdge(msg.getSender());  // edge along which reject was sent
                log.debug("Received " + ((Reject) msg));
                this.rejectedEdges.add(e);
                this.basicEdges.remove(e);  // no longer a basic edge if reject is sent
            } else if (msg instanceof Report) {
                Report reportMsg = ((Report) msg);
                log.debug("Received " + reportMsg);
                updateMWOE(reportMsg);
                if (receivedReportsFromChildren()) {
                    handleReports();
                }
            } else if (msg instanceof ChangeRoot) { // note current leader can't receive a changeroot message
                // check if my id is adjacent to mwoe
                ChangeRoot crMsg = ((ChangeRoot) msg);
//                log.debug("Received " + crMsg);
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
            } else if (msg instanceof Connect) {
                Connect connectMsg = ((Connect) msg);
                mergeOrAbsorb(connectMsg);
            }
        }
    }

    private void transition() throws InterruptedException, BrokenBarrierException {
        barrier.await();
        processSendBuffer();
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

                if (this.basicEdges.isEmpty()) {
                    log.info("Branch edges: " + branchEdges);
                    sendTerminationToMaster();
                    selfKill = true;
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
