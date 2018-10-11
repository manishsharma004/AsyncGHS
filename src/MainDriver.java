public class MainDriver {

    public static void main(String args[]) {
        GraphGenerator graph = new GraphGenerator(0);
        MasterThread master = new MasterThread("Master of Puppets", 0, graph.getAdj());
        master.start();
    }

}