import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.*;

public class server {
  public static void main(String[] args) {
    try (ServerSocket ss = new ServerSocket(1234)) {
      System.out.println("Server up on 1234");

      while (true) {
        try {
          Socket s = ss.accept();
          System.out.println("New connection. Hello");
          (new Thread(new serverWorker(s))).start();

        } catch (IOException | NullPointerException e) {
          System.out.println("Error: " + e.getMessage() + ". Goodbye");
        }
      }
    } catch (IOException e) {
      System.out.println("Error: " + e.getMessage() + ". Goodbye");
    }
  }
}

class serverWorker implements Runnable {
  Socket socket;

  serverWorker(Socket s) {
    this.socket = s;
  }

  @Override
  public void run() {

    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

      int value = 0;
      while (true) {
        String input = reader.readLine();
        String[] query = input.split(" ");
        String operation = query[0];

        if (operation.equals("balance"))
          writer.println(value);
        else if (operation.equals("movement")) {
          int amount = Integer.parseInt(query[1]);

          if (amount + value >= 0) {
            value += amount;
            writer.println("true");
          } else if (amount + value < 0)
            writer.println("false");
        } else
          writer.println("405 Method Not Allowed");
      }
    } catch (IOException e) {
      System.out.println("Worker Error: " + e.getMessage() + ". Goodbye");
    }
  }
}