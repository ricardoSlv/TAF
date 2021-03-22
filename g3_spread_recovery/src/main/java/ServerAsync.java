import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

import spread.AdvancedMessageListener;
import spread.MembershipInfo;
import spread.SpreadConnection;
import spread.SpreadException;
import spread.SpreadGroup;
import spread.SpreadMessage;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ServerAsync {

  private static void handleMessage(int[] money, boolean[] active, ArrayList<SpreadMessage> delayedMessages,
      SpreadConnection conn, SpreadMessage msg) {
    byte[] data = msg.getData();
    String message = new String(data, StandardCharsets.UTF_8);
    String[] messageBits = message.split(" ");
    SpreadMessage rep = new SpreadMessage();

    String result = "";
    System.out.println(message);
    if (messageBits[0].equals("state")) {
      if (active[0] == false) {
        int stateReceived = Integer.parseInt(messageBits[1]);
        money[0] = stateReceived;
        active[0] = true;
        delayedMessages.forEach(m -> handleMessage(money, active, delayedMessages, conn, m));
        delayedMessages.clear();
      }
    } else if (active[0]) {
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
    } else {
      delayedMessages.add(msg);
    }
  }

  public static void main(String[] args) {

    int serverId = Integer.parseInt(args[0]);
    ScheduledExecutorService es = Executors.newScheduledThreadPool(1);

    int[] money = { 1000 };
    boolean[] active = { false };
    ArrayList<SpreadMessage> delayedMessages = new ArrayList<>();
    SpreadConnection conn = new SpreadConnection();

    try {
      conn.connect(InetAddress.getByName("localhost"), 4803, "server" + serverId, false, true);
    } catch (Exception e) {
      e.printStackTrace();
    }
    conn.add(new AdvancedMessageListener() {
      @Override
      public void regularMessageReceived(SpreadMessage msg) {
        handleMessage(money, active, delayedMessages, conn, msg);
      }

      @Override
      public void membershipMessageReceived(SpreadMessage msg) {
        MembershipInfo info = msg.getMembershipInfo();
        if (info.isRegularMembership() && info.isCausedByJoin()) {

          SpreadGroup newMember = info.getJoined();
          SpreadGroup[] members = info.getMembers();

          System.out.println("New member:" + newMember.toString());
          System.out.println("Members:" + members.length);

          SpreadMessage rep = new SpreadMessage();
          String resp = "state " + money[0];
          rep.setData(resp.getBytes(StandardCharsets.UTF_8));
          rep.setReliable();
          rep.addGroup(newMember);
          try {
            // Check if im an active member or i'm alone, wich are the cases where i should
            // transfer my state
            if (members.length > 1 && active[0] || members.length == 1)
              conn.multicast(rep);
          } catch (SpreadException e) {
            e.printStackTrace();
          }
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
