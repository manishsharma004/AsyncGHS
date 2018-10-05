public class MainDriver {

    public static void main(String args[]) {
        GraphGenerator graph = new GraphGenerator();
        MasterThread master = new MasterThread("Master of Puppets", 0, graph.getAdj(), 3);
        master.start();
    }

}