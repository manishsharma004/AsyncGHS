package asyncGHS;

import java.util.*;

public class GraphGenerator {
    private Map<Integer, List<MainDriver.Process>> adj = new HashMap<>();

    public GraphGenerator() {
        /**
         * Generates a graph of provided size using random integers.
         */
        List<MainDriver.Process> list = new ArrayList<>();
        list.add(new MainDriver.Process(2, (float)1.0));
        list.add(new MainDriver.Process(6, (float)3.0));
        this.adj.put(1, list);

        list = new ArrayList<>();
        list.add(new MainDriver.Process(1, (float)1.0));
        list.add(new MainDriver.Process(3, (float)3.0));
        this.adj.put(2, list);

        list = new ArrayList<>();
        list.add(new MainDriver.Process(2, (float)3.0));
        list.add(new MainDriver.Process(4, (float)2.0));
        this.adj.put(3, list);

        list = new ArrayList<>();
        list.add(new MainDriver.Process(3, (float)2.0));
        list.add(new MainDriver.Process(5, (float)1.0));
        list.add(new MainDriver.Process(7, (float)1.0));
        this.adj.put(4, list);

        list = new ArrayList<>();
        list.add(new MainDriver.Process(4, (float)1.0));
        list.add(new MainDriver.Process(6, (float)2.0));
        list.add(new MainDriver.Process(7, (float)4.0));
        this.adj.put(5, list);

        list = new ArrayList<>();
        list.add(new MainDriver.Process(1, (float)3.0));
        list.add(new MainDriver.Process(5, (float)2.0));
        list.add(new MainDriver.Process(7, (float)3.0));
        this.adj.put(6, list);

        list = new ArrayList<>();
        list.add(new MainDriver.Process(4, (float)1.0));
        list.add(new MainDriver.Process(5, (float)4.0));
        list.add(new MainDriver.Process(6, (float)3.0));
        this.adj.put(7, list);

    }

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
