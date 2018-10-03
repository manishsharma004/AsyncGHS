import java.util.HashMap;

public class MainDriver {

    public static void main(String args[]){

        MasterThread master = new MasterThread("Master", 0);
//
//        int numWorkers = 4;
//        Processes[] workers = new Processes[numWorkers];
//        for (int i = 0; i < workers.length; i++) {
//            workers[i] = new Processes(("thread-"+i), i);
//        }
//        //assign neighbors to input threads and then start the threads
//        for(int i = 0; i < workers.length; i++) {
//            //assigning only two neighbor to the thread 0 has neighbor 1 and 2, 1 has neighbor 2 and 3, 2 has neighbor 3 and 0
//            // 3 has neighbor 0 and 1
//            HashMap<Integer, Processes> neighbor = new HashMap<>();
//            neighbor.put((i+1)%4, workers[(i+1)%4]);
//            neighbor.put((i+2)%4, workers[(i+2)%4]);
//            workers[i].assignNeighbors(neighbor, master);
//        }

        master.start();
//        for (int i = 0; i < workers.length; i++) {
//            workers[i].start();
//        }
    }
}
