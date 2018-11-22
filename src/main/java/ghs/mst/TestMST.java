package ghs.mst;

import edu.princeton.cs.algs4.EdgeWeightedGraph;
import edu.princeton.cs.algs4.In;

public class TestMST {
    public static void main(String[] args) {
        // read graph
        In in = new In(args[0]);
        EdgeWeightedGraph G = new EdgeWeightedGraph(in);
        MasterThread masterThread = new MasterThread("MASTER", G);
        masterThread.start();
    }
}
