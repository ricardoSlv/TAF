import java.util.Random;
import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class TesterAsync {

  private static void runOperations(ScheduledExecutorService es, NettyMessagingService ms, final int port,
      final int serverp1, final int serverp2, final int value, final int number) {

    System.out.println(number);
    if (number == 0)
      System.out.println("Value: " + value);
    else {
      Random rand = new Random();
      String[] operations = { "balance", "movement" };
      String operation = operations[rand.nextInt(2)];
      // value*[-100,100]
      int accountMovement = (int) (Math.round(50 * (-2.0 + rand.nextFloat() * 4.0)));

      String customType = port + "" + number + " " + operation;
      // second arg is ignored for balance
      byte[] operationMsg = (port + "" + number + " " + accountMovement).getBytes();

      Boolean[] active = { true };
      ms.registerHandler(customType, (address, msg) -> {
        if (active[0]) {
          synchronized (active) {
            active[0] = false;
          }
          ms.unregisterHandler(operation);
          if (operation == "movement" && msg[0] != 0)
            runOperations(es, ms, port, serverp1, serverp2, value + accountMovement, number - 1);
          else
            runOperations(es, ms, port, serverp1, serverp2, value, number - 1);
        }

      }, es);

      ms.sendAsync(Address.from("localhost", serverp1), operation, operationMsg);
      ms.sendAsync(Address.from("localhost", serverp2), operation, operationMsg);
    }
  }

  public static void main(String[] args) {

    int port = Integer.parseInt(args[0]);
    int serverp1 = Integer.parseInt(args[1]);
    int serverp2 = Integer.parseInt(args[2]);
    System.out.println("PORTS->" + port + serverp1 + serverp2);
    ScheduledExecutorService es = Executors.newScheduledThreadPool(1);
    NettyMessagingService ms = new NettyMessagingService("bank", Address.from(port), new MessagingConfig());

    try {
      Thread.sleep(100, 0);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    ms.start();

    runOperations(es, ms, port, serverp1, serverp2, 0, 10_000);
  }
}