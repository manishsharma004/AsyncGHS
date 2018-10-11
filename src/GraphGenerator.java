import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates a bi-directional graph with vertices having unique IDs (integers, for simplicity).
 */
// TODO: read from input file, input file first line: number of nodes in vertex, second line comma separated uids
    // third line onwards adjacency matrix
public class GraphGenerator {

    private Map<Integer, List<Integer>> adj = new HashMap<>();

    public GraphGenerator() {
        /**
         * Generates a graph of provided size using random integers.
         */
        // TODO: generate random connected simple graph
        this.adj.put(1, Arrays.asList(10, 11, 8));
        this.adj.put(10, Arrays.asList(1));
        this.adj.put(11, Arrays.asList(1, 6, 20));
        this.adj.put(6, Arrays.asList(8, 11, 20));
        this.adj.put(8, Arrays.asList(1, 15, 6));
        this.adj.put(15, Arrays.asList(8));
        this.adj.put(20, Arrays.asList(6, 11));
    }

    public GraphGenerator(int edges) {
       adj = RandomEdgeGraph.createGraph();
    }

    public GraphGenerator(Map<Integer, List<Integer>> adj) {
        this.adj = adj;
    }

    public Map<Integer, List<Integer>> getAdj() {
        return adj;
    }

    public void setAdj(Map<Integer, List<Integer>> adj) {
        this.adj = adj;
    }
}
