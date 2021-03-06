import java.util.Random;

import myutils.AsyncStub;

public class TesterAsync {

  private static void runOperations(AsyncStub msgService, final int value, final int number) {

    System.out.println("Op number: " + number);
    if (number == 0)
      System.out.println("Value: " + value);
    else {
      Random rand = new Random();
      // String[] operations = { "balance", "movement" };
      // String operation = operations[rand.nextInt(2)];
      // value*[-100,100]
      int accountMovement = (int) (Math.round(50 * (-2.0 + rand.nextFloat() * 4.0)));

      msgService.sendAndReceiveAsync(number, "echo", accountMovement).thenAccept(msg -> {
        System.out.println(new String(msg));
        try {
          Thread.sleep(10);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        runOperations(msgService, value + accountMovement, number - 1);
      });
    }
  }

  public static void main(String[] args) throws InterruptedException {

    int clientId = Integer.parseInt(args[0]);
    System.out.println("My Id -> " + clientId);

    AsyncStub msgService = new AsyncStub(clientId);

    runOperations(msgService, 0, 10_000);

    Thread.sleep(Integer.MAX_VALUE);
  }
}