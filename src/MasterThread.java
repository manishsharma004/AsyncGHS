import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * The master thread is responsible for spawning and synchronizing the worker threads.
 */
public class MasterThread extends Thread {

    int id;
    BlockingQueue<Message> queue = new LinkedBlockingDeque<>();
    Process[] workers;
    int numWorkers = 0;
    int round = 0;
    CyclicBarrier barrier;
    HashSet<Integer> roundCompletedThreads = new HashSet<>();   // threads that finished current round
    HashSet<Integer> terminatedThreads = new HashSet<Integer>();
    Map<Integer, List<Integer>> graph;
    Map<Integer, Integer> nodeIdToProcessMap;
    Map<Integer, Integer> nodeToParentMapping;

    public void setWorkers(Process[] workers) {
        this.workers = workers;
    }

    public MasterThread(String name, int id, Map<Integer, List<Integer>> graph) {
        super(name);
        this.id = id;
        this.graph = graph;
        this.barrier = new CyclicBarrier(this.graph.size());
        this.nodeToParentMapping = new HashMap<>();
    }

    @Override
    public synchronized void start() {
        super.start();
    }

    private synchronized boolean pushToQueue(Process p, Message m) {
        return p.queue.add(m);
    }

    synchronized public boolean broadcastMessage(Message msg) {

        for (int i = 0; i < this.numWorkers; i++) {
            pushToQueue(this.workers[i], msg);
        }
        return true;
    }

    /**
     * Handles maxId received from workers. The workers can either terminate or choose to continue to the next
     * round. If a worker thread sends a terminate signal to the master, it is terminated and the resources are
     * freed. If it chooses to continue, the master thread awaits messages from all non-terminated threads to give a
     * go-ahead for the next round. When the master receives a terminate signal from all workers, it shuts down.
     */
    synchronized public void handleMessage() throws InterruptedException {
        Message out = queue.take();
        switch (out.getType()) {
            case END_ROUND:
                this.roundCompletedThreads.add(out.sender);
                break;

            case TERMINATE:
                this.terminatedThreads.add(out.sender);
                this.roundCompletedThreads.add(out.sender);
                nodeIdToProcessMap.put(out.sender, out.getMaxId());
                break;

            default:
                break;
        }
    }

    synchronized public boolean hasCurrentRoundTerminated() {
        if (this.numWorkers <= this.roundCompletedThreads.size()) {
            this.roundCompletedThreads = new HashSet<Integer>();
            System.out.println("All workers have finished round " + this.round + ". Starting next round");
            return true;
        }
        return false;
    }

    synchronized public boolean haveAllThreadsTerminated() {
        System.out.println("terminated Threads size : " + this.terminatedThreads.size());
        if (this.numWorkers <= this.terminatedThreads.size()) {
            return true;
        } else {
            return false;
        }
    }

    synchronized public boolean startNewRound() {
        this.round += 1;
        System.out.println("\n\nBroadcast Message to start Round " + this.round);
        this.broadcastMessage(new Message(0, this.round, MessageType.START_ROUND));
        return false;
    }

    synchronized public void waitForAllWorkersCompletion() throws InterruptedException {
        while (true) {
            handleMessage();
            if (hasCurrentRoundTerminated() || haveAllThreadsTerminated()) {
                System.out.println("All the workers have terminated or round has terminated");
                break;
            }
        }
    }

    public void spawnWorkers() {
        int numProcesses = this.graph.keySet().size();
        Process[] processes = new Process[numProcesses];

        // spawn processes
        Set<Integer> keySet = this.graph.keySet();
        Iterator<Integer> keySetIterator = keySet.iterator();
        int index = 0;
        this.nodeIdToProcessMap = new HashMap<>();
        while (keySetIterator.hasNext()) {
            Integer vertexId = keySetIterator.next();
            processes[index] = new Process("thread-" + vertexId, vertexId, barrier);
            this.nodeIdToProcessMap.put(vertexId, index);
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
                adjacentProcesses.add(processes[this.nodeIdToProcessMap.get(neighborVertex)]);
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

    public void printTree() {}
    @Override
    public void run() {
        try {
            spawnWorkers();
            System.out.println("Workers spawned");
            while (!haveAllThreadsTerminated()) {
                startNewRound();
                waitForAllWorkersCompletion();
            }
            printTree();
            System.out.println("Terminating " + this.getName());
        } catch (InterruptedException e) {
        }
    }
}