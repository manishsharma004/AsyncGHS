package asyncGHS;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public class MainDriver {

    public static class Process {
        Integer id;
        Float weight;

        public Process(Integer id, Float weight) {
            this.id = id;
            this.weight = weight;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public Float getWeight() {
            return weight;
        }

        public void setWeight(Float weight) {
            this.weight = weight;
        }
    }

    private static Logger log = Logger.getLogger("Main");

    public static Map<Integer, List<Process>> readInput(String pathToAdjacencyList) throws IOException {
        File file = new File(pathToAdjacencyList);
        BufferedReader br = new BufferedReader(new FileReader(file));
        String st;
        int count = 0;
        int noOfNodes = 0;
        String[] workers;
        String[] neighborList;
        Map<Integer, List<Process>> adj = new HashMap<>();

        // first line: No of Nodes
        if((st = br.readLine()) != null) {
            noOfNodes = Integer.parseInt(st);
        }

        workers = new String[noOfNodes];
        // second line: worker ids
        if ((st = br.readLine()) != null) {
            workers = st.split("\\s+");
//            log.info("Processes UIDs = " + Arrays.toString(workers));
        }

        // next noOfNodes lines - adjacency matrix
        while ((st = br.readLine()) != null) {
            neighborList = st.split("\\s+");
            List<Process> neighbors = new ArrayList<>();
            for (int i = 0; i < noOfNodes; i++) {
                if (!neighborList[i].equals("-1")) {
                    neighbors.add(new MainDriver.Process(Integer.parseInt(workers[i]), Float.parseFloat(neighborList[i])));
                }
            }
            adj.put(Integer.parseInt(workers[count]), neighbors);
            count++;
        }

//        log.info("Adjacency list = " + adj);
        return adj;
    }

    public static void main(String[] args) throws IOException {
        Map<Integer, List<Process>> adj;
        GraphGenerator graph;
        if (args.length < 1) {
//            System.out.println("Format: java MainDriver <input file path>");
//            System.exit(-1);
            graph = new GraphGenerator();
        }
        else {
             adj = readInput(args[0]);
             graph = new GraphGenerator(adj);
        }
        graph.printGraph();
    }
}
