package asyncGHS;

import sun.applet.Main;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GraphGenerator {
    private Map<Integer, List<MainDriver.Process>> adj = new HashMap<>();

//    public GraphGenerator() {
//        /**
//         * Generates a graph of provided size using random integers.
//         */
////        this.adj.put(1, Arrays.asList(10, 11, 8));
////        this.adj.put(10, Arrays.asList(1));
////        this.adj.put(11, Arrays.asList(1, 6, 20));
////        this.adj.put(6, Arrays.asList(8, 11, 20));
////        this.adj.put(8, Arrays.asList(1, 15, 6));
////        this.adj.put(15, Arrays.asList(8));
////        this.adj.put(20, Arrays.asList(6, 11));
//    }

    public GraphGenerator(Map<Integer, List<MainDriver.Process>> adj) {
        this.adj = adj;
    }

    public Map<Integer, List<MainDriver.Process>> getAdj() {
        return adj;
    }

    @Override
    public String toString() {

        return "GraphGenerator{" +
                "adj=" + adj +
                '}';
    }

    public void printGraph() {
        for (Integer in: adj.keySet()) {
            System.out.print(in + " --> ");
            for (MainDriver.Process p : adj.get(in)) {
                System.out.print("{ " + p.getId() + ", " + p.weight + " } ");
            }
            System.out.println("");
        }
    }
}
