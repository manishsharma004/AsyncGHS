import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class MasterThread extends Thread {
    /**
     * The master thread is responsible for spawning and synchronizing the worker threads.
     */
    int id;
    BlockingQueue<Message> queue = new LinkedBlockingDeque<>();
    Process[] workers;
    int numWorkers = 0;
    int round = 0;
    int diameter;
    HashSet<Integer> roundCompletedThreads = new HashSet<>();   // threads that finished current round
    HashSet<Integer> terminatedThreads = new HashSet<Integer>();
    Map<Integer, List<Integer>> graph;  // TODO: make a new class for undirected graph and use that
    // Why do we need this map? We have (say) N workers stored in an array.
    // worker[0] represents vertex id (say) 12 in the graph, worker[1] may represent 200.
    // We need to know the vertex id (12 or 200 in this case) to the index (0 or 1, respectively) in this case, so
    // when we look up a certain vertex id (200), we know which worker (1) to go to
    Map<Integer, Integer> vertexIdToIndexMap;

    public void setWorkers(Process[] workers) {
        this.workers = workers;
    }

    public MasterThread(String name, int id, Map<Integer, List<Integer>> graph, int diameter) {
        super(name);
        this.id = id;
        this.graph = graph;
        this.diameter = diameter;
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

    synchronized public void handleMessage() throws InterruptedException {
        /**
         * Handles message received from workers. The workers can either terminate or choose to continue to the next
         * round. If a worker thread sends a terminate signal to the master, it is terminated and the resources are
         * freed. If it chooses to continue, the master thread awaits messages from all non-terminated threads to give a
         * go-ahead for the next round. When the master receives a terminate signal from all workers, it shuts down.
         */
        Message out = queue.take();
        switch (out.getType()) {
            case END_ROUND:
                this.roundCompletedThreads.add(out.sender);
                break;

            case TERMINATE:
                this.terminatedThreads.add(out.sender);
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
        if (this.numWorkers <= this.terminatedThreads.size()) {
            return true;
        } else {
            return false;
        }
    }

    synchronized public boolean startNewRound() {
        this.round += 1;
        this.broadcastMessage(new Message(0, 0, this.round, MessageType.START_ROUND));
        return false;
    }

    synchronized public void waitForAllWorkersCompletion() throws InterruptedException {
        while (true) {
            handleMessage();
            if (hasCurrentRoundTerminated() || haveAllThreadsTerminated()) {
                break;
            }
        }
    }

    public void spawnWorkers() {
        int numWorkers = this.graph.keySet().size();
        Process[] workers = new Process[numWorkers];

        // spawn processes
        Set<Integer> keySet = this.graph.keySet();
        Iterator<Integer> keySetIterator = keySet.iterator();
        int index = 0;
        this.vertexIdToIndexMap = new HashMap<>();
        while (keySetIterator.hasNext()) {
            Integer vertexId = keySetIterator.next();
            workers[index] = new Process("thread-" + vertexId, vertexId);
            this.vertexIdToIndexMap.put(vertexId, index);
            index += 1;
        }

        // assign neighbours and set diameter
        for (int i = 0; i < workers.length; i++) {
            // get vertex id of i-th worker
            Integer vertexId = workers[i].getUid();
            // get neighbours of this vertex
            List<Integer> neighborVertexIds = this.graph.get(vertexId);
            // get workers that correspond to these neighbors
            List<Process> adjacentProcesses = new ArrayList<>();
            for (Integer neighborVertex : neighborVertexIds) {
                adjacentProcesses.add(workers[this.vertexIdToIndexMap.get(neighborVertex)]);
            }
            workers[i].setNeighbors(adjacentProcesses);
            workers[i].setDiameter(this.diameter);
            workers[i].setMaster(this);
        }

        // start all workers
        for (int i = 0; i < workers.length; i++) {
            workers[i].start();
        }

        this.workers = workers;
        this.numWorkers = numWorkers;
    }

    @Override
    public void run() {
        try {
            spawnWorkers();
            System.out.println("Workers spawned");
            while (!haveAllThreadsTerminated()) {
                startNewRound();
                waitForAllWorkersCompletion();
            }
            System.out.println("Terminating " + this.getName());
        } catch (InterruptedException e) {
        }
    }
}