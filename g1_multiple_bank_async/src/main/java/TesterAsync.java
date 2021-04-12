import java.util.Random;

import myutils.AsyncStub;

public class TesterAsync {

  private static void runOperations(AsyncStub msgService, final int value, final int number) {

    System.out.println(number);
    if (number == 0)
      System.out.println("Value: " + value);
    else {
      Random rand = new Random();
      String[] operations = { "balance", "movement" };
      String operation = operations[rand.nextInt(2)];
      // value*[-100,100]
      int accountMovement = (int) (Math.round(50 * (-2.0 + rand.nextFloat() * 4.0)));

      msgService.sendAndReceiveAsync(number, operation, accountMovement).thenAccept(msg -> {
        if (operation == "movement" && msg[0] != 0)
          runOperations(msgService, value + accountMovement, number - 1);
        else
          runOperations(msgService, value, number - 1);
      });
    }
  }

  public static void main(String[] args) {

    int port = Integer.parseInt(args[0]);
    int serverp1 = Integer.parseInt(args[1]);
    int serverp2 = Integer.parseInt(args[2]);
    System.out.println("PORTS->" + port + serverp1 + serverp2);

    AsyncStub msgService = new AsyncStub(port, serverp1, serverp2);

    runOperations(msgService, 0, 10_000);
  }
}