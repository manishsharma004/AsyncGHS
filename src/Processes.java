
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class Processes extends Thread{
    BlockingQueue<Message> q = new LinkedBlockingDeque<>(10);
    int id;
    HashMap<Integer, Processes> neighbors;
    int count = 0;

    public Processes(String name, int id) {
        super(name);
        this.id = id;
    }

    public Processes(String name, int id, HashMap<Integer, Processes> neighbors) {
        super(name);
        this.id = id;
        this.neighbors = neighbors;
    }

    public void assignNeighbors(HashMap<Integer, Processes> neighbors) {
        this.neighbors = neighbors;
    }

    @Override
    public synchronized void start() {
        super.start();
    }

    //push message to queue of any given thread(Process)
    private synchronized boolean pushToQueue(Processes p, Message m) {
        return p.q.add(m);
    }

    //
    synchronized  public boolean putMessage(Message msg) {
        //need to check if the receiver is the neighbor of the process
        if (this.neighbors.containsKey(msg.receiver)) {
            return pushToQueue(this.neighbors.get(msg.receiver), msg);
        }
        return false;
    }

    synchronized  public Message getMessage() throws InterruptedException{
        //this is to stimulate adding message to neighbors queue
        int sender, receiver;
        String m;
        sender = id;
        receiver = (id +1)%4;
        m = "msg" + sender + "-" + receiver;
        Message newm = new Message(sender, receiver, m);
        putMessage(newm);

        receiver = (id +2)%4;
        m = "msg" + sender + "-" + receiver;
        newm = new Message(sender, receiver, m);
        putMessage(newm);

        //this is the actual getMessage
        Message out = q.take();
        this.count = this.count +1;
        return out;
    }

    @Override
    public void run() {
        try {
            while (true) {
                //Currently for test run, to limit sending message to just 3 rounds
                if (count > 3 ){
                    break;
                }

                //add a new message in neioghbors queue and gets message from current queue
                Message x = getMessage();
                if (x == null) {
                    break;
                }
                System.out.println(this.getName() + " : " + x.toString());

            }
        } catch (InterruptedException e) {
        }
    }
}