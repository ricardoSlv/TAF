import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.*;
import java.util.Random;

public class tester {
  public static void main(String[] args) {

    try (Socket s = new Socket("localhost", 1234)) {
      System.out.println("Tester running on 1234");

      BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
      PrintWriter writer = new PrintWriter(new OutputStreamWriter(s.getOutputStream()), true);

      writer.println("balance");
      int value = Integer.parseInt(reader.readLine());

      Random rand = new Random();
      String[] operations = { "balance", "movement" };
      for (int i = 0; i < 1_000_000; i++) {
        String operation = operations[rand.nextInt(2)];

        if (operation.equals("balance")) {
          writer.println(operation);
          int serverValue = Integer.parseInt(reader.readLine());
          if (serverValue != value)
            System.out.println("Error on server value");
        } else if (operation.equals("movement")) {
          // value*[-2,2]
          int accountMovement = (int) (50 * Math.round(-2.0 + rand.nextFloat() * 4.0));
          writer.println(operation + " " + accountMovement);
          String result = reader.readLine();

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