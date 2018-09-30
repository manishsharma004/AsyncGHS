import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MasterThread {

    public static void main(String args[]) {

        int numWorkers = 4;
        Process[] workers = new Process[numWorkers];
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new Process(("thread-" + i), i);
        }

        // TODO: read a text file to load the graph and assign neighbours
        // below is a sample graph of diameter 2
        Map neighbors = new HashMap();
        neighbors.put(0, Arrays.asList(workers[1], workers[2], workers[3]));
        neighbors.put(1, Arrays.asList(workers[0]));
        neighbors.put(2, Arrays.asList(workers[0], workers[3]));
        neighbors.put(3, Arrays.asList(workers[2], workers[0]));

        for (int i = 0; i < workers.length; i++) {
            workers[i].setNeighbors(neighbors);
            workers[i].setDiameter(2);
        }

        for (int i = 0; i < workers.length; i++) {
            workers[i].start();
        }
    }
}
