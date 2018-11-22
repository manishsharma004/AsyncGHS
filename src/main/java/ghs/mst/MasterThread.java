package ghs.mst;

import edu.princeton.cs.algs4.Edge;
import edu.princeton.cs.algs4.EdgeWeightedGraph;
import ghs.message.Exit;
import ghs.message.Message;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * The {@code MasterThread} represents the daemon process that spawns and terminates workers, and prints info
 * about the minimum spanning tree before exiting.
 */
public class MasterThread extends Thread {
    private static Logger log = Logger.getLogger("Master");

    public BlockingQueue<Exit> queue = new LinkedBlockingDeque<>();
    private int numWorkers;
    private Process[] workers;
    private CyclicBarrier barrier;
    private Set<Integer> terminatedThreads = new HashSet<Integer>();

    // MST info
    private EdgeWeightedGraph graph;
    private Set<Edge> mstEdges = new HashSet<>();
    private int leaderId;
    private Edge coreEdge;

    /**
     * Initializes a new MasterThread.
     *
     * @param name  Name of the daemon
     * @param graph a graph with edge weights
     */
    public MasterThread(String name, EdgeWeightedGraph graph) {
        super(name);
        this.graph = graph;
        this.barrier = new CyclicBarrier(this.graph.V());
    }

    /**
     * Spawns workers and assigns neighbors as in the graph.
     */
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

    /**
     * Helper function to send a {@code Message} to a worker.
     *
     * @param p Process instance
     * @param m Message instance
     */
    private void pushToQueue(Process p, Message m) {
        p.queue.add(m);
    }

    /**
     * Broadcasts a {@code Message} to all workers.
     *
     * @param msg Message instance
     */
    private void broadcastMessage(Message msg) {
        for (int i = 0; i < this.numWorkers; i++) {
            pushToQueue(this.workers[i], msg);
        }
    }

    /**
     * Checks if all threads are ready to terminate.
     *
     * @return true if ready
     */
    private boolean receivedExitFromAllWorkers() {
        return this.numWorkers <= this.terminatedThreads.size();
    }

    /**
     * Handles the {@code Exit} message received from worker and updates the MST info.
     *
     * @throws InterruptedException
     */
    private void handleMessage() throws InterruptedException {
        if (!this.queue.isEmpty()) {
            Exit exitMsg = this.queue.take();

            if (exitMsg.isLeader()) {
                this.leaderId = exitMsg.getSender();
            }
            this.coreEdge = exitMsg.getCoreEdge();
            this.mstEdges.addAll(exitMsg.getBranchEdges());
            // the only message master can receive is terminate, along with branch edges
            this.terminatedThreads.add(exitMsg.getSender());
        }
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    public void run() {
        try {
            spawnWorkers();
            log.info("Workers spawned.");

            while (!receivedExitFromAllWorkers()) {
                // wait for workers to send you EXIT messages
                handleMessage();
            }
            log.info("All threads have sent EXIT.");

            // terminate workers, i.e., broadcast EXIT to all workers (id doesn't matter)
            // workers exit when they receive this message
            Exit killMsg = new Exit(-1);
            broadcastMessage(killMsg);

            // print edges in MST and weight of MST
            log.info("Final MST edges=" + this.mstEdges +
                    ", leader=" + this.leaderId +
                    ", id (core edge)=" + this.coreEdge);

            // wait for workers to receive KILL signal and shut down before exiting
            // otherwise, the barrier may interfere with some threads exiting
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
