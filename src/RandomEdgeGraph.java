import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

public class RandomEdgeGraph
{
    Map<Integer, List<Integer>> adjacencyList;

    public RandomEdgeGraph()
    {
        adjacencyList = new HashMap<Integer, List<Integer>>();
        for (int i = 1; i <= 6; i++)
            adjacencyList.put(i, new LinkedList<Integer>());
    }

    public RandomEdgeGraph(int v)
    {
        adjacencyList = new HashMap<Integer, List<Integer>>();
        for (int i = 1; i <= v; i++)
            adjacencyList.put(i, new LinkedList<Integer>());
    }

    public void setEdge(int to, int from)
    {
        if (to > adjacencyList.size() || from > adjacencyList.size())
            System.out.println("The vertices does not exists");

        List<Integer> sls = adjacencyList.get(to);
        sls.add(from);
        List<Integer> dls = adjacencyList.get(from);
        dls.add(to);
    }

    public List<Integer> getEdge(int to)
    {
        if (to > adjacencyList.size())
        {
            System.out.println("The vertices does not exists");
            return null;
        }
        return adjacencyList.get(to);
    }

    public static Map<Integer, List<Integer>>  createGraph() {
        System.out.println("Enter the number of edges: ");
        Scanner sc = new Scanner(System.in);
        int e = sc.nextInt();
        RandomEdgeGraph reg = new RandomEdgeGraph();
        try
        {
            int minV = (int) Math.ceil((1 + Math.sqrt(1 + 8 * e)) / 2);
            int maxV = e + 1;

            Random random = new Random();
            int v = Math.abs(random.nextInt(maxV - minV) + minV);
            System.out.println("Random graph has "+v+" vertices");

            reg = new RandomEdgeGraph(v);
            int count = 1, to, from;
            while (count <= e)
            {
                to = Math.abs(random.nextInt(v + 1 - 1) + 1);
                from = Math.abs(random.nextInt(v + 1 - 1) + 1);

                reg.setEdge(to, from);
                count++;
            }

            System.out
                    .println("THe Adjacency List Representation of the random graph is: ");

            for (int i = 1; i <= v; i++)
            {
                System.out.print(i + " -> ");
                List<Integer> edgeList = reg.getEdge(i);
                if (edgeList.size() == 0){
                    System.out.print("null");
                }

                else
                {
                    for (int j = 1;; j++)
                    {
                        if (j != edgeList.size())
                            System.out.print(edgeList.get(j - 1) + " -> ");
                        else {
                            System.out.print(edgeList.get(j - 1));
                            break;
                        }
                    }
                }
                System.out.println();
            }
        }
        catch (Exception E)
        {
            System.out.println("Something went wrong");
        }
        sc.close();
        return reg.adjacencyList;
    }

    public static void main(String args[])
    {
        System.out.println("Enter the number of edges: ");

        Scanner sc = new Scanner(System.in);
        int e = sc.nextInt();
        try
        {
            int minV = (int) Math.ceil((1 + Math.sqrt(1 + 8 * e)) / 2);
            int maxV = e + 1;

            Random random = new Random();
            int v = Math.abs(random.nextInt(maxV - minV) + minV);
            System.out.println("Random graph has "+v+" vertices");

            RandomEdgeGraph reg = new RandomEdgeGraph(v);
            int count = 1, to, from;
            while (count <= e)
            {
                to = Math.abs(random.nextInt(v + 1 - 1) + 1);
                from = Math.abs(random.nextInt(v + 1 - 1) + 1);

                reg.setEdge(to, from);
                count++;
            }

            System.out
                    .println("THe Adjacency List Representation of the random graph is: ");

            for (int i = 1; i <= v; i++)
            {
                System.out.print(i + " -> ");
                List<Integer> edgeList = reg.getEdge(i);
                if (edgeList.size() == 0)
                    System.out.print("null");
                else
                {
                    for (int j = 1;; j++)
                    {
                        if (j != edgeList.size())
                            System.out.print(edgeList.get(j - 1) + " -> ");
                        else {
                            System.out.print(edgeList.get(j - 1));
                            break;
                        }
                    }
                }
                System.out.println();
            }
        }
        catch (Exception E)
        {
            System.out.println("Something went wrong");
        }
        sc.close();
    }

}
