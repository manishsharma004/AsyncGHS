package floodmax;

import org.apache.log4j.Logger;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * The master thread is responsible for spawning and synchronizing the worker threads.
 */
public class MasterThread extends Thread {
    private static Logger log = Logger.getLogger("Master");
    BlockingQueue<Message> queue = new LinkedBlockingDeque<>();
    // states
    private int numWorkers = 0;
    private int round = 0;
    private Process[] workers;
    private CyclicBarrier barrier;
    private HashSet<Integer> roundCompletedThreads = new HashSet<>();   // threads that finished current round
    private HashSet<Integer> terminatedThreads = new HashSet<Integer>();
    private Map<Integer, List<Integer>> graph;
    private Map<Integer, HashSet<Integer>> parentToNodeMap;

    public MasterThread(String name, int id, Map<Integer, List<Integer>> graph) {
        super(name);
        this.graph = graph;
        this.barrier = new CyclicBarrier(this.graph.size());
        this.parentToNodeMap = new HashMap<>();
    }

    @Override
    public void start() {
        super.start();
    }

    private void pushToQueue(Process p, Message m) {
        p.queue.add(m);
    }

    private void broadcastMessage(Message msg) {
        for (int i = 0; i < this.numWorkers; i++) {
            pushToQueue(this.workers[i], msg);
        }
    }

    /**
     * Handles maxId received from workers. The workers can either terminate or choose to continue to the next
     * round. If a worker thread sends a terminate signal to the master, it is terminated and the resources are
     * freed. If it chooses to continue, the master thread awaits messages from all non-terminated threads to give a
     * go-ahead for the next round. When the master receives a terminate signal from all workers, it shuts down.
     */
    private void handleMessage() throws InterruptedException {
        Message out = queue.take();
        switch (out.getType()) {
            case END_ROUND:
                this.roundCompletedThreads.add(out.sender);
                break;

            case TERMINATE:
                this.terminatedThreads.add(out.sender);
                this.roundCompletedThreads.add(out.sender);
                HashSet<Integer> adj = parentToNodeMap.getOrDefault(out.getMaxId(), new HashSet<>());
                adj.add(out.sender);
                parentToNodeMap.put(out.getMaxId(), adj);
                break;

            default:
                break;
        }
    }

    private boolean hasCurrentRoundTerminated() {
        if (this.numWorkers <= this.roundCompletedThreads.size()) {
            this.roundCompletedThreads = new HashSet<Integer>();
            log.info("All workers have finished round " + this.round + ". Starting next round.");
            return true;
        }
        return false;
    }

    private boolean haveAllThreadsTerminated() {
        log.debug("Terminated threads size = " + this.terminatedThreads.size());
        return this.numWorkers <= this.terminatedThreads.size();
    }

    private boolean startNewRound() {
        this.round += 1;
        log.info("Broadcast Message to start Round " + this.round);
        this.broadcastMessage(new Message(0, this.round, MessageType.START_ROUND));
        return false;
    }

    private void waitForAllWorkersCompletion() throws InterruptedException {
        while (true) {
            handleMessage();
            if (hasCurrentRoundTerminated()) {
                log.info("All the workers have terminated or round has terminated");
                break;
            }
        }
    }

    private void spawnWorkers() {
        int numProcesses = this.graph.keySet().size();
        Process[] processes = new Process[numProcesses];

        // spawn processes
        Set<Integer> keySet = this.graph.keySet();
        Iterator<Integer> keySetIterator = keySet.iterator();
        int index = 0;
        Map<Integer, Integer> nodeIdToProcessMap = new HashMap<>();
        while (keySetIterator.hasNext()) {
            Integer vertexId = keySetIterator.next();
            processes[index] = new Process("thread-" + vertexId, vertexId, barrier);
            nodeIdToProcessMap.put(vertexId, index);
            index += 1;
        }

        // assign neighbours
        for (int i = 0; i < processes.length; i++) {
            // get vertex id of i-th worker
            Integer vertexId = processes[i].getUid();
            // get neighbours of this vertex
            List<Integer> neighborVertexIds = this.graph.get(vertexId);
            // get workers that correspond to these neighborProcesses
            List<Process> adjacentProcesses = new ArrayList<>();
            for (Integer neighborVertex : neighborVertexIds) {
                adjacentProcesses.add(processes[nodeIdToProcessMap.get(neighborVertex)]);
            }
            processes[i].setNeighborProcesses(adjacentProcesses);
            processes[i].setMaster(this);
        }

        // start all workers
        for (int i = 0; i < processes.length; i++) {
            processes[i].start();
        }

        this.workers = processes;
        this.numWorkers = numProcesses;
    }

    /**
     * Prints the final BFS tree as an adjacency list
     */
    private void printTree() {
        log.info("Leader is " + this.parentToNodeMap.get(-1));  // leader is the root of the tree
        log.info("Adjacency list: " + this.parentToNodeMap);
    }

    private void terminateAllThreads() {
        this.broadcastMessage(new Message(0, MessageType.KILL));
    }

    @Override
    public void run() {
        try {
            spawnWorkers();
            while (!haveAllThreadsTerminated()) {
                startNewRound();
                waitForAllWorkersCompletion();
            }

            printTree();
            terminateAllThreads();
            log.debug("Terminating " + this.getName());
        } catch (InterruptedException e) {
        }
    }
}