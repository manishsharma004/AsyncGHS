import java.io.*;
import java.util.*;

public class MainDriver {

    public static Map<Integer, List<Integer>> readInput(Integer noOfNodes, String pathToAdjancencyList) throws IOException  {
        File file = new File(pathToAdjancencyList);
        BufferedReader br = new BufferedReader(new FileReader(file));
        String st;
        int count = 0;
        String[] workers  = new String[noOfNodes];
        String[] neighborList;
        Map<Integer, List<Integer>> adj = new HashMap<>();

        //Reading Worker Ids
        if ((st = br.readLine()) != null) {
            workers = st.split("\\s+");
            System.out.println(Arrays.toString(workers));
            st = br.readLine();
        }

        //Reading Adjacency Matrix
        while ((st = br.readLine()) != null){
            neighborList = st.split("\\s+");
            List<Integer> neighbors = new ArrayList<>();
            for (int  i = 0; i < noOfNodes; i++) {
                if (neighborList[i].equals("1")) {
                    neighbors.add(Integer.parseInt(workers[i]));
                }
            }
            adj.put(Integer.parseInt(workers[count]), neighbors);
            count++;
        }

        System.out.println(adj);
        return adj;
    }

    public static void main(String args[]) throws IOException {

        if (args.length < 3) {
            System.out.println("Input should be of format MainDriver __No_Of_Nodes__ --path __Path_To_Input_File__ ");
            System.exit(-1);
        }

        int noOfNodes = Integer.parseInt(args[0]);
        Map<Integer, List<Integer>> adj = readInput(noOfNodes, args[2]);
        GraphGenerator graph = new GraphGenerator(adj);
        MasterThread master = new MasterThread("Master of Puppets", 0, graph.getAdj());
        master.start();
    }

}