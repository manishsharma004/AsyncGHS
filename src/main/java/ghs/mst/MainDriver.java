package ghs.mst;

import edu.princeton.cs.algs4.EdgeWeightedGraph;
import edu.princeton.cs.algs4.In;

import java.util.logging.Logger;

public class MainDriver {
    private static Logger log = Logger.getLogger("Main");

    public static void main(String[] args) {
        // read graph
        In in = new In(args[0]);
        EdgeWeightedGraph G = new EdgeWeightedGraph(in);
        MasterThread masterThread = new MasterThread("MASTER", G);
        masterThread.start();
    }
}
