import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

import spread.BasicMessageListener;
import spread.SpreadConnection;
import spread.SpreadException;
import spread.SpreadGroup;
import spread.SpreadMessage;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ServerAsync {

  public static void main(String[] args) {

    int serverId = Integer.parseInt(args[0]);
    ScheduledExecutorService es = Executors.newScheduledThreadPool(1);

    int[] money = { 1000 };

    SpreadConnection conn = new SpreadConnection();
    try {
      conn.connect(InetAddress.getByName("localhost"), 4803, "server" + serverId, false, false);
    } catch (Exception e) {
      e.printStackTrace();
    }

    conn.add(new BasicMessageListener() {
      @Override
      public void messageReceived(SpreadMessage msg) {
        byte[] data = msg.getData();
        String message = new String(data, StandardCharsets.UTF_8);
        String[] messageBits = message.split(" ");
        SpreadMessage rep = new SpreadMessage();

        String result = "";
        if (messageBits[1].equals("balance")) {
          result = "" + money[0];
        } else if (messageBits[1].equals("movement")) {

          int movementValue = Integer.parseInt(messageBits[2]);

          result = "false";
          if (money[0] + movementValue >= 0) {
            result = "true";
            money[0] += movementValue;
          }
        }

        String resp = messageBits[0] + " " + result;
        rep.setData(resp.getBytes(StandardCharsets.UTF_8));
        rep.setReliable();
        rep.addGroup(msg.getSender());
        try {
          conn.multicast(rep);
        } catch (SpreadException e) {
          e.printStackTrace();
        }
      }
    });

    SpreadGroup group = new SpreadGroup();
    try {
      group.join(conn, "servers");
    } catch (SpreadException e) {
      e.printStackTrace();
    }

    int[] counter = { 0 };
    es.scheduleAtFixedRate(() -> {
      System.out.println(counter[0] + ": Server " + serverId + " has " + money[0] + "€");
      counter[0] += 1;
    }, 0, 2, TimeUnit.SECONDS);
  }
}
