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
    HashSet<Integer> currentRoundTerminatedThreads = new HashSet<>();   // threads that finished current round
    HashSet<Integer> terminatedThreads = new HashSet<Integer>();
    int numOfTerminatedThreads = 0;
    Map<Integer, List<Integer>> graph;  // TODO: make a new class for undirected graph and use that


    public MasterThread(String name, int id, Map<Integer, List<Integer>> graph) {
        super(name);
        this.id = id;
        this.graph = graph;
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
        if (out.getType() == MessageType.TERMINATE && !terminatedThreads.contains(out.sender)) {
            this.terminatedThreads.add(out.sender);
            this.numOfTerminatedThreads += 1;
        }
        switch (out.getType()) {
            case TERMINATE:
                this.terminatedThreads.add(out.sender);
                this.numOfTerminatedThreads += 1;
                break;

            case END_ROUND:
                // TODO: needs testing, might be missing some edge case
                this.currentRoundTerminatedThreads.add(out.sender);
                // if everyone has finished except those terminated, start new round
                if (this.currentRoundTerminatedThreads.size() == (this.numWorkers - this.numOfTerminatedThreads)) {
                    // broadcast message to all processes to start next round
                    this.broadcastMessage(new Message(0, 0, "", MessageType.START_ROUND));
                    this.round += 1;
                    this.currentRoundTerminatedThreads.clear();
                }
                break;
        }
    }

    public void spawnWorkers() {
        int numWorkers = this.graph.keySet().size();
        Process[] workers = new Process[numWorkers];

        // spawn processes
        Set<Integer> keySet = this.graph.keySet();
        Iterator<Integer> keySetIterator = keySet.iterator();
        int index = 0;

        // Why do we need this map? We have (say) N workers stored in an array.
        // worker[0] represents vertex id (say) 12 in the graph, worker[1] may represent 200.
        // We need to know the vertex id (12 or 200 in this case) to the index (0 or 1, respectively) in this case, so
        // when we look up a certain vertex id (200), we know which worker (1) to go to
        Map<Integer, Integer> vertexIdToIndexMap = new HashMap<>();

        while (keySetIterator.hasNext()) {
            Integer vertexId = keySetIterator.next();
            workers[index] = new Process("thread-" + vertexId, vertexId);
            vertexIdToIndexMap.put(vertexId, index);
            index += 1;
        }

        // assign neighbours
        for (int i = 0; i < workers.length; i++) {
            // get vertex id of i-th worker
            Integer vertexId = workers[i].getUid();
            // get neighbours of this vertex
            List<Integer> neighborVertexIds = this.graph.get(vertexId);
            // get workers that correspond to these neighbors
            List<Process> adjacentProcesses = new ArrayList<>();
            for (Integer neighborVertex : neighborVertexIds) {
                adjacentProcesses.add(workers[vertexIdToIndexMap.get(neighborVertex)]);
            }
            workers[i].setNeighbors(adjacentProcesses);
        }

        // start all workers
        for (int i = 0; i < workers.length; i++) {
            workers[i].start();
        }

        this.workers = workers;
        this.numWorkers = numWorkers;
        // we use 0 for sender and receiver ids when we broadcast, consider it dummy
        this.broadcastMessage(new Message(0, 0, "", MessageType.START_ROUND));
    }

    @Override
    public void run() {
        try {
            spawnWorkers();
            System.out.println("Workers spawned");
            while (true) {
                if (this.numWorkers <= this.numOfTerminatedThreads) {
                    System.out.println("All workers terminated. Terminating master now...");
                    break;
                }

                handleMessage();
            }
        } catch (InterruptedException e) {
        }
    }
}