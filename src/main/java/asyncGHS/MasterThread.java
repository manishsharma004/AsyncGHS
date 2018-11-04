package asyncGHS;

import floodmax.MessageType;
import ghs.message.MasterMessage;
import ghs.message.Message;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.LinkedBlockingDeque;

public class MasterThread extends Thread {
    private static Logger log = Logger.getLogger("Master");

    private int numWorkers = 0;
    private int round = 0;
    private Process[] workers;
    private CyclicBarrier barrier;
    BlockingQueue<MasterMessage> queue = new LinkedBlockingDeque<>();
    private HashSet<Integer> roundCompletedThreads = new HashSet<>();   // threads that finished current round
    private HashSet<Integer> terminatedThreads = new HashSet<Integer>();
    private Map<Integer, List<NeighborObject>> graph;
    private Map<Integer, HashSet<Integer>> parentToNodeMap;

    public MasterThread(String name, int id, Map<Integer, List<NeighborObject>> graph) {
        super(name);
        this.graph = graph;
        this.barrier = new CyclicBarrier(this.graph.size());
        this.parentToNodeMap = new HashMap<>();
    }

    private void spawnWorkers() {
        int numProcesses = this.graph.keySet().size();
        Process[] processes = new Process[numProcesses];

        // spawn processes
        //NodeIdProcessMap<Key, Value> : process Key is stored in value location of Processes Array
        int index = 0;
        Map<Integer, Integer> nodeIdToProcessMap = new HashMap<>();
        for (Integer vertexId : this.graph.keySet()) {
            processes[index] = new Process("thread-" + vertexId, vertexId, barrier);
            nodeIdToProcessMap.put(vertexId, index);
            index += 1;
        }

        // assign neighbours
        for (int i = 0; i < processes.length; i++) {
            // get vertex id of i-th worker
            Integer vertexId = processes[i].getUid();
            // get neighbours of this vertex
            List<NeighborObject> neighbors = this.graph.get(vertexId);
            // get workers that correspond to these neighborProcesses
            HashMap<NeighborObject, Process> adjacentProcesses = new HashMap<>();
            for (NeighborObject neighborObject : neighbors) {
                adjacentProcesses.put(neighborObject, processes[nodeIdToProcessMap.get(vertexId)]);
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

    private void pushToQueue(Process p, Message m) {
        p.masterQueue.add(m);
    }

    private void broadcastMessage(Message msg) {
        for (int i = 0; i < this.numWorkers; i++) {
            pushToQueue(this.workers[i], msg);
        }
    }

    private boolean haveAllThreadsTerminated() {
        log.debug("Terminated threads size = " + this.terminatedThreads.size());
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
        log.info("Received " + out);
        switch (out.getType()) {
            case END_ROUND:
                this.roundCompletedThreads.add(out.getSender());
                break;

            case TERMINATE:
                this.terminatedThreads.add(out.getSender());
                this.roundCompletedThreads.add(out.getSender());
                HashSet<Integer> adj = parentToNodeMap.getOrDefault(out.getMsg(), new HashSet<>());
                adj.add(out.getSender());
                parentToNodeMap.put(out.getMsg(), adj);
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

    private void waitForAllWorkersCompletion() throws InterruptedException {
        while (true) {
            handleMessage();
            if (hasCurrentRoundTerminated()) {
                log.info("All the workers have terminated or round has terminated");
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
            System.out.println("child threads started");
            //trying to terminate after round 2
            while (!haveAllThreadsTerminated()) {
                startNewRound();
                waitForAllWorkersCompletion();
            }
            terminateAllThreads();
            log.debug("Terminating " + this.getName());
        }
        catch(InterruptedException e) {

        }
    }
}
