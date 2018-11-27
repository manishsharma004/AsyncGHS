# AsynchGHS

*A distributed algorithm to compute minimum spanning tree in asynchronous networks.*

To know more about how the algorithm works, see [Distributed Algorithms by Nancy Lynch](https://www.amazon.com/Distributed-Algorithms-Kaufmann-Management-Systems/dp/1558603484).

#### Simulating an asynchronous network
We simulate an asynchronous network by adding random delays (less than 20 time units) to messages. However, the order of sending and processing of messages is preserved. For example, if a process `p` sends two messages, `m1` and `m2` to process `q` in that order, `q` shall process `m1` first followed by `m2`.

#### How to Run
*Requires Java 8*

Using **Ant:** Extract the compressed file and run `ant run -Darg0=in/tinyEWG.txt`.

Using **Maven:** Import the project in IntelliJ Idea as a Maven project and run `TestMST.java` with command line arguments `in/tinyEWG.txt`.