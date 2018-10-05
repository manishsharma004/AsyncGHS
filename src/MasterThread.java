import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class MasterThread extends Thread{

    BlockingQueue<Message> q = new LinkedBlockingDeque<>();
    int id;
    Processes[] workers;
    int numWorkers = 0;
    HashSet<Integer> terminatedThreads = new HashSet<Integer>();
    int numOfTerminatedThreads = 0;
    int round = 0;
    HashSet<Integer> roundCompletedThreads = new HashSet<Integer>();
    int noOfThreadsCompletedRound = 0;

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

    synchronized  public Message handleMessage(Message msg) throws InterruptedException {
        if (msg == null) return null;
        System.out.println(this.getName() + " : " + msg.toString());

        switch(msg.messageType) {
            case END_ROUND:
                if (!this.roundCompletedThreads.contains(msg.sender)) {
                    this.roundCompletedThreads.add(msg.sender);
                    this.noOfThreadsCompletedRound += 1;
                }
                break;
            case TERMINATE:
                if(!this.terminatedThreads.contains(msg.sender) && !this.workers[msg.sender].isAlive()) {
                    this.terminatedThreads.add(msg.sender);
                    this.numOfTerminatedThreads += 1;
                }
            default: break;
        }
        return msg;
    }

    synchronized  public Message getMessage() throws InterruptedException{
        //this is the actual getMessage
        return handleMessage(q.take());
    }

    synchronized public void startWorkerThreads() {
        int numWorkers = 3;
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
    }

    synchronized public boolean checkForRoundTermination() {
        if (this.numWorkers <= this.noOfThreadsCompletedRound) {
            this.noOfThreadsCompletedRound = 0;
            this.roundCompletedThreads = new HashSet<Integer>();
            System.out.println("All Children Threads Have Completed the Current Round, Master Threads also will start new round");
            return true;
        }
        return false;
    }

    synchronized public boolean checkForThreadTermination() {
        for (int i = 0; i < workers.length; i++) {
            if (workers[i].isAlive()) {
                return false;
            }
        }
        return true;
    }

    synchronized public boolean startNewRound() {
        this.round += 1;
        this.broadcastMessage(new Message(0, 0, this.round, MessageType.START_ROUND));
        return false;
    }

    synchronized public void waitForAllWorkerCompletion() throws InterruptedException {
        while(true){
            Message x = getMessage();
            if (x == null) {
                continue;
            }
            if(checkForRoundTermination()) {
                break;
            }
            Thread.sleep(1000);
        }
    }

    @Override
    public void run() {
        try {
            startWorkerThreads();
            while (!checkForThreadTermination()) {
                startNewRound();
                waitForAllWorkerCompletion();
            }
            if (checkForThreadTermination()) {
                if (this.numWorkers <= this.numOfTerminatedThreads){
                    System.out.println("All Children Threads Have Terminated, Master Threads also Terminating");
                }
            }
            System.out.println("Terminating Master");
        } catch (InterruptedException e) {
        }
    }
}
