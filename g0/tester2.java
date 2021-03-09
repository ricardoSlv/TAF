import java.io.IOException;
import java.util.Random;

import myutils.EasySocket;

public class tester2 {
  public static void main(String[] args) {

    try (EasySocket s = new EasySocket("localhost", 1234)) {
      System.out.println("Tester running on 1234");

      s.println("balance");
      int value = Integer.parseInt(s.readLine());

      Random rand = new Random();
      String[] operations = { "balance", "movement" };
      for (int i = 0; i < 1_000_000; i++) {
        String operation = operations[rand.nextInt(2)];

        if (operation.equals("balance")) {
          s.println(operation);
          int serverValue = Integer.parseInt(s.readLine());
          if (serverValue != value)
            System.out.println("Error on server value");
        } else if (operation.equals("movement")) {
          // value*[-2,2]
          int accountMovement = (int) (50 * Math.round(-2.0 + rand.nextFloat() * 4.0));
          s.println(operation + " " + accountMovement);
          String result = s.readLine();

          if (accountMovement + value >= 0)
            if (result == "false")
              System.out.println("Error on server movement +");
            else
              value += accountMovement;
          else if (accountMovement + value < 0)
            if (result == "true") {
              value += accountMovement;
              System.out.println("Error on server movement +");
            }
          if (i % 100_000 == 0) {
            System.out.println("Iteration => " + i);
            System.out.println("Value => " + value);
            System.out.println("Movement => " + accountMovement);
            System.out.println("Result => " + result);
          }
        }
      }
    } catch (IOException e) {
      System.out.println("Error: " + e.getMessage() + ". Goodbye");
    }
  }
}
