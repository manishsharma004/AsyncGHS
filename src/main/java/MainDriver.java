import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public class MainDriver {

    private static Logger log = Logger.getLogger("Main");

    public static Map<Integer, List<Integer>> readInput(Integer noOfNodes,
                                                        String pathToAdjacencyList)
            throws IOException {
        File file = new File(pathToAdjacencyList);
        BufferedReader br = new BufferedReader(new FileReader(file));
        String st;
        int count = 0;
        String[] workers = new String[noOfNodes];
        String[] neighborList;
        Map<Integer, List<Integer>> adj = new HashMap<>();

        // first line: worker ids and second line: blank
        if ((st = br.readLine()) != null) {
            workers = st.split("\\s+");
            log.info("Processes UIDs = " + Arrays.toString(workers));
            st = br.readLine();
        }

        // next noOfNodes lines - adjacency matrix
        while ((st = br.readLine()) != null) {
            neighborList = st.split("\\s+");
            List<Integer> neighbors = new ArrayList<>();
            for (int i = 0; i < noOfNodes; i++) {
                if (neighborList[i].equals("1")) {
                    neighbors.add(Integer.parseInt(workers[i]));
                }
            }
            adj.put(Integer.parseInt(workers[count]), neighbors);
            count++;
        }

        log.info("Adjacency list = " + adj);
        return adj;
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.out.println("Format: java MainDriver <no of nodes> --path <input file path>");
            System.exit(-1);
        }

        int noOfNodes = Integer.parseInt(args[0]);
        Map<Integer, List<Integer>> adj = readInput(noOfNodes, args[2]);
        GraphGenerator graph = new GraphGenerator(adj);
        MasterThread master = new MasterThread("MASTER", 0, graph.getAdj());
        master.start();
    }
}