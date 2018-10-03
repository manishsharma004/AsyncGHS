import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class MasterThread extends Thread{

    BlockingQueue<Message> q = new LinkedBlockingDeque<>();
    int id;
    Processes[] workers;
    int count = 0;
    int numWorkers = 0;
    HashSet<Integer> terminatedThreads = new HashSet<Integer>();
    int numOfTerminatedThreads = 0;

    public MasterThread(String name, int id) {
        super(name);
        this.id = id;
    }

    public MasterThread(String name, int id, Processes[] workers, int n) {
        super(name);
        this.id = id;
        this.workers = workers;
        this.numWorkers = n;
    }

    public void assignWorkers(Processes[] workers, int n) {
        this.workers = workers;
        this.numWorkers = n;
    }

    @Override
    public synchronized void start()
    {
        super.start();
    }

    //push message to queue of any given thread(Process)
    private synchronized boolean pushToQueue(Processes p, Message m) {
        return p.q.add(m);
    }

    //Broadcast message to all workers
    synchronized  public boolean broadcastMessage(Message msg) {
        //need to check if the receiver is the neighbor of the process
        for (int i  = 0;i < this.numWorkers; i++) {
            pushToQueue(this.workers[i], msg);
        }
        return true;
    }

    synchronized  public Message getMessage() throws InterruptedException{

        //this is the actual getMessage
        Message out = q.take();
        if (out.message.equals("COMPLETED") && !terminatedThreads.contains(out.sender)) {
            this.terminatedThreads.add(out.sender);
            this.numOfTerminatedThreads += 1;
        }
        this.count = this.count +1;
        return out;
    }

    public void startChildThreads() {
        int numWorkers = 4;
        Processes[] workers = new Processes[numWorkers];
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new Processes(("thread-"+i), i);
        }
        //assign neighbors to input threads and then start the threads
        for(int i = 0; i < workers.length; i++) {
            //assigning only two neighbor to the thread 0 has neighbor 1 and 2, 1 has neighbor 2 and 3, 2 has neighbor 3 and 0
            //3 has neighbor 0 and 1
            HashMap<Integer, Processes> neighbor = new HashMap<>();
            neighbor.put((i+1)%numWorkers, workers[(i+1)%numWorkers]);
            neighbor.put((i+2)%numWorkers, workers[(i+2)%numWorkers]);
            workers[i].assignNeighbors(neighbor, this);
        }

        for (int i = 0; i < workers.length; i++) {
            workers[i].start();
        }

        this.numWorkers = numWorkers;
        this.workers = workers;
        this.broadcastMessage(new Message(0, 0, "BroadCast Message From Master"));
    }

    @Override
    public void run() {
        try {
            startChildThreads();
            System.out.println("Child Threads started");
            while (true) {

                //Currently for test run, to limit sending message to just 3 rounds
                if (this.numWorkers <= this.numOfTerminatedThreads){
                    System.out.println("All Children Threads Have Terminated, Master Threads also Terminating");
                    break;
                }

                //add a new message in neighbors queue and gets message from current queue
                Message x = getMessage();
                if (x == null) {
                    continue;
                }
                System.out.println(this.getName() + " : " + x.toString());
                Thread.sleep(1000);

            }
        } catch (InterruptedException e) {
        }
    }
}
