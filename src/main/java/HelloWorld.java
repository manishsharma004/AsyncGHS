import ghs.message.Message;
import ghs.message.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

public class HelloWorld {

    public static void main(String args[]) {
        PriorityBlockingQueue<Message> queue = new PriorityBlockingQueue<>(10, Message::compareTo);
        List<Message> list = new ArrayList<>();
        try {
//            Thread thread = new Thread(() -> {
//                System.out.println("Polling...");
//                while (true) {
//                    try {
////                        Message poll = queue.take();
//                        queue.drainTo(list);
//                        for (Message m : list) {
//                            System.out.println();
//                        }
////                        System.out.println("Polled: " + poll);
//                    }
//                    catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//            });

//            thread.start();

//            Thread.sleep(TimeUnit.SECONDS.toMillis(5));
            System.out.println("Adding to queue");


            list.add(new Test(2, 710, 1, 1));
            list.add(new Test(3, 5, 1, 1));
            list.add(new Test(4, 61, 1, 1));
            list.add(new Test(2, 1, 1, 1));
            list.add(new Test(5, 2, 1, 1));
            list.add(new Test(6, 6, 1, 1));
            list.add(new Test(7, 7, 1, 1));

            queue.addAll(list);
            Thread.sleep(TimeUnit.SECONDS.toMillis(1));
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
