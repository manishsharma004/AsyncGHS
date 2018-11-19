package asyncGHS;

import edu.princeton.cs.algs4.Edge;
import edu.princeton.cs.algs4.EdgeWeightedGraph;
import floodmax.MessageType;
import ghs.message.MasterMessage;
import ghs.message.Message;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.LinkedBlockingDeque;

public class MasterThread extends Thread {
    private static Logger log = Logger.getLogger("Master");
    public BlockingQueue<MasterMessage> queue = new LinkedBlockingDeque<>();
    private int numWorkers = 0;
    private int round = 0;
    private Process[] workers;
    private CyclicBarrier barrier;
    private HashSet<Integer> roundCompletedThreads = new HashSet<>();   // threads that finished current round
    private HashSet<Integer> terminatedThreads = new HashSet<Integer>();
    private EdgeWeightedGraph graph;

    public MasterThread(String name, EdgeWeightedGraph graph) {
        super(name);
        this.graph = graph;
        this.barrier = new CyclicBarrier(this.graph.V());
    }

    private void spawnWorkers() {
        int numProcesses = this.graph.V();
        Process[] processes = new Process[numProcesses];

        // spawn processes, the vertices in the graph are named 0 to V-1
        for (int i = 0; i < numProcesses; i++) {
            List<Edge> edges = new ArrayList<>();
            for (Edge e : this.graph.adj(i)) {
                edges.add(e);
            }
            processes[i] = new Process("thread-" + i, i, edges, this.barrier);
            processes[i].setMaster(this);
        }

        // assign neighbor Processes
        for (int i = 0; i < numProcesses; i++) {
            List<Process> neighbors = new ArrayList<>();
            for (Edge e : this.graph.adj(i)) {
                int neighborId = e.other(i);    // vertex at the other end of the edge
                neighbors.add(processes[neighborId]);
            }
            processes[i].setNeighborProcesses(neighbors);
        }

        // start all workers
        for (int i = 0; i < processes.length; i++) {
            processes[i].start();
        }

        this.workers = processes;
        this.numWorkers = numProcesses;
    }

    private void pushToQueue(Process p, Message m) {
        p.masterQueue.add(m);
    }

    private void broadcastMessage(Message msg) {
        for (int i = 0; i < this.numWorkers; i++) {
            pushToQueue(this.workers[i], msg);
        }
    }

    private boolean haveAllThreadsTerminated() {
//        log.debug("Terminated threads size = " + this.terminatedThreads.size());
        return this.numWorkers <= this.terminatedThreads.size();
    }

    private boolean startNewRound() {
        this.round += 1;
        log.info("Broadcast Message to start Round " + this.round);
        this.broadcastMessage(new MasterMessage(0, this.round, MessageType.START_ROUND));
        return false;
    }

    /**
     * Handles maxId received from workers. The workers can either terminate or choose to continue to the next
     * round. If a worker thread sends a terminate signal to the master, it is terminated and the resources are
     * freed. If it chooses to continue, the master thread awaits messages from all non-terminated threads to give a
     * go-ahead for the next round. When the master receives a terminate signal from all workers, it shuts down.
     */
    private void handleMessage() throws InterruptedException {
        MasterMessage out = queue.take();
//        log.info("Received " + out);
        switch (out.getType()) {
            case END_ROUND:
                this.roundCompletedThreads.add(out.getSender());
                break;

            case TERMINATE:
                this.terminatedThreads.add(out.getSender());
                this.roundCompletedThreads.add(out.getSender());
                break;

            default:
                break;
        }
    }

    private boolean hasCurrentRoundTerminated() {
        if (this.numWorkers <= this.roundCompletedThreads.size()) {
            this.roundCompletedThreads = new HashSet<Integer>();
//            log.info("All workers have finished round " + this.round + ". Starting next round.");
            return true;
        }
        return false;
    }

    private void waitForAllWorkersCompletion() throws InterruptedException {
        while (true) {
            handleMessage();
            if (hasCurrentRoundTerminated()) {
//                log.info("All the workers have terminated or round has terminated");
                break;
            }
        }
    }

    private void terminateAllThreads() {
        this.broadcastMessage(new MasterMessage(0, MessageType.KILL));
    }

    @Override
    public synchronized void start() {
        super.start();
    }

    @Override
    public void run() {
        try {
            spawnWorkers();
            log.info("Workers spawned.");
            // trying to terminate after round 2
            while (!haveAllThreadsTerminated()) {
                startNewRound();
                waitForAllWorkersCompletion();
            }
            terminateAllThreads();
            log.debug("Terminating " + this.getName());
        } catch (InterruptedException e) {

        }
    }
}
