
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class Processes extends Thread{
    BlockingQueue<Message> q = new LinkedBlockingDeque<>();
    int uid;
    int maxUid = Integer.MAX_VALUE;
    HashMap<Integer, Processes> neighbors;
    MasterThread master;
    int count = 0;
    int round = 0;

    public Processes(String name, int uid) {
        super(name);
        this.uid = uid;
    }

    public Processes(String name, int uid, HashMap<Integer, Processes> neighbors) {
        super(name);
        this.uid = uid;
        this.neighbors = neighbors;
    }

    public void assignNeighbors(HashMap<Integer, Processes> neighbors, MasterThread master) {
        this.neighbors = neighbors;
        this.master = master;
    }

    public void setMaster(MasterThread master) {
        this.master = master;
    }

    @Override
    public synchronized void start() {
        super.start();
    }

    synchronized  public Message getMessage() throws InterruptedException{
        //this is the actual getMessage
        Message out = this.q.take();
        out = handleMessage(out);
        this.count = this.count +1;
        return out;
    }

    synchronized  public Message handleMessage(Message msg) throws InterruptedException {
        if (msg == null) return null;
        System.out.println(this.getName() + " : " + msg.toString());

        switch(msg.messageType) {
            case START_ROUND:
                round = msg.message;
                break;
            default: break;
        }
        return msg;
    }

    //push message to queue of any given thread(Process)
    private synchronized boolean pushToQueue(Processes p, Message m) {
        return p.q.add(m);
    }

    private synchronized boolean pushToQueue(MasterThread p, Message m) {
        return p.q.add(m);
    }

    synchronized public boolean sendMessageToMaster(Message msg) {
        return pushToQueue(this.master, msg);
    }

    synchronized public boolean putMessage(Message msg) {
        //need to check if the receiver is the neighbor of the process
        if (this.neighbors.containsKey(msg.receiver)) {
            return pushToQueue(this.neighbors.get(msg.receiver), msg);
        }
        return false;
    }

    synchronized public boolean broadcastMessageToAllNeighbors(int message, MessageType msgType) {
        this.neighbors.forEach((key, value) -> {
            pushToQueue(value, new Message(this.uid, key, message, msgType));
        });
        return true;
    }

    public void waitUntilMasterStartsNewRound() throws InterruptedException {
        while(true) {
            Message msg = this.q.take();
            System.out.println(this.getName() + " : " + msg.toString());

            if (msg == null || msg.messageType != MessageType.START_ROUND) continue;

            if (msg.messageType.equals(MessageType.START_ROUND)) {
                if (msg.message > this.round ) {
                    this.round = msg.message;
                }
                else {
                    throw new InterruptedException("Received round which is smaller than current round");
                }
                return;
            }
            else {
                this.q.add(msg);
            }
        }
    }

    public void sendRoundCompletionMessageToMaster() {
        this.sendMessageToMaster(new Message(this.uid, 0, this.round,  MessageType.END_ROUND));
    }

    public void processFloodMax() throws InterruptedException {

        broadcastMessageToAllNeighbors(this.uid, MessageType.EXPLORE);

        int countOfExploreFromNeighbors = 0;
        while (countOfExploreFromNeighbors < this.neighbors.size()) {
            Message msg = this.q.take();
            if (msg == null ) { continue; }
            System.out.println(this.getName() + " : " + msg.toString());
            if (msg.messageType.equals(MessageType.EXPLORE)) {
                countOfExploreFromNeighbors += 1;
                if (this.maxUid < msg.message) {
                    System.out.println(this.getName() + " : Max Uid Updated to " + msg.message + " by : " + msg.toString());
                    this.maxUid = msg.message;
                }
            }
        }
        this.count += 1;
    }

    @Override
    public void run() {
        try {
            while(true) {
                if (this.count > 1){
                    sendMessageToMaster(new Message(this.uid, 0, 0, MessageType.TERMINATE));
                    break;
                }
                waitUntilMasterStartsNewRound();
                processFloodMax();
                sendRoundCompletionMessageToMaster();
            }
            System.out.println("Terminating " + this.getName());
        } catch (InterruptedException e) {
        }
    }
}