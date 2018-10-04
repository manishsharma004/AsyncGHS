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
    HashSet<Integer> terminatedThreads = new HashSet<Integer>();
    int numOfTerminatedThreads = 0;
    Map<Integer, List<Process>> graph;  // TODO: make a new class for undirected graph and use that


    public MasterThread(String name, int id, Map<Integer, List<Process>> graph) {
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
        // TODO: handle the above logic
        Message out = queue.take();
        if (out.message == -99 && !terminatedThreads.contains(out.sender)) {
            this.terminatedThreads.add(out.sender);
            this.numOfTerminatedThreads += 1;
        }
    }

    public void spawnWorkers() {
        int numWorkers = this.graph.keySet().size();
        Process[] workers = new Process[numWorkers];

        // assign neighbors by reading the graph
        Set<Integer> keySet = this.graph.keySet();
        Iterator<Integer> keySetIterator = keySet.iterator();
        int i = 0;
        while (keySetIterator.hasNext()) {
            Integer vertexId = keySetIterator.next();
            workers[i] = new Process("thread-" + vertexId, vertexId, this.graph.get(vertexId));
            i += 1;
        }

        for (i = 0; i < workers.length; i++) {
            workers[i].start();
        }

        this.workers = workers;
        this.numWorkers = numWorkers;
        this.broadcastMessage(new Message(0, 0, -1));
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