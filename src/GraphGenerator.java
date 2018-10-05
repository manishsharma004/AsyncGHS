import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GraphGenerator {
    /**
     * Generates a bi-directional graph with vertices having unique IDs (integers, for simplicity).
     */
    private Map<Integer, List<Integer>> adj = new HashMap<>();

    public GraphGenerator() {
        /**
         * Generates a graph of provided size using random integers.
         */
        // TODO: generate random connected simple graph
        this.adj.put(1, Arrays.asList(10, 11, 8, 6));
        this.adj.put(10, Arrays.asList(1));
        this.adj.put(11, Arrays.asList(1, 6));
        this.adj.put(6, Arrays.asList(8, 11));
        this.adj.put(8, Arrays.asList(1, 15));
        this.adj.put(15, Arrays.asList(8));
    }

    public Map<Integer, List<Integer>> getAdj() {
        return adj;
    }

    public void setAdj(Map<Integer, List<Integer>> adj) {
        this.adj = adj;
    }
}
