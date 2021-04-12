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

import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;

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
    NettyMessagingService ms = new NettyMessagingService("bank", Address.from(5000), new MessagingConfig());

    int[] money = { 1000 };
    boolean[] active = { false };
    ArrayList<SpreadMessage> delayedMessages = new ArrayList<>();
    SpreadConnection conn = new SpreadConnection();

    ArrayList<String> ancestors = new ArrayList<>();
    boolean[] imTheleader = { false };

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
        if (info.isRegularMembership()) {

          if (info.isCausedByJoin() && info.getJoined().toString().equals(conn.getPrivateGroup().toString()))
            for (SpreadGroup m : info.getMembers())
              if (m.toString().equals(conn.getPrivateGroup().toString()) == false)
                ancestors.add(m.toString());

          if (info.isCausedByLeave() || info.isCausedByDisconnect())
            ancestors.remove(info.getLeft().toString());

          if (ancestors.size() == 0 && imTheleader[0] == false) {
            imTheleader[0] = true;
            System.out.println("I'm the leader");
            ms.start();
            ms.registerHandler("echo", (address, msge) -> {
              String[] msgParts = new String(msge, StandardCharsets.UTF_8).split(" ");
              String msgID = msgParts[0];
              System.out.println("ola");
              ms.sendAsync(address, msgID + " echo", msgParts[1].getBytes(StandardCharsets.UTF_8)).exceptionally(t -> {
                System.out.println(t.getMessage());
                return null;
              });
            }, es);
          } else if (imTheleader[0] == false)
            System.out.println("Ancestors: " + String.join(" ", ancestors));
          else
            System.out.println("I'm still the leader");
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
    // Prevent Thread from dying
    es.scheduleAtFixedRate(() -> {
      // System.out.println(counter[0] + ": Server " + serverId + " has " + money[0] +
      // "€");
      counter[0] += 1;
    }, 0, 2, TimeUnit.SECONDS);
  }
}

// import java.math.BigInteger;
// import java.nio.charset.StandardCharsets;

// import io.atomix.cluster.messaging.MessagingConfig;
// import io.atomix.cluster.messaging.impl.NettyMessagingService;
// import io.atomix.utils.net.Address;

// import java.util.concurrent.Executors;
// import java.util.concurrent.ScheduledExecutorService;
// import java.util.concurrent.TimeUnit;

// public class ServerAsync {

// public static void main(String[] args) {

// int port = Integer.parseInt(args[0]);
// ScheduledExecutorService es = Executors.newScheduledThreadPool(1);
// NettyMessagingService ms = new NettyMessagingService("bank",
// Address.from(port), new MessagingConfig());

// try {
// Thread.sleep(100, 0);
// } catch (InterruptedException e) {
// e.printStackTrace();
// }

// ms.start();
// int[] money = { 1000 };

// ms.registerHandler("balance", (address, msg) -> {
// String msgID = new String(msg, StandardCharsets.UTF_8).split(" ")[0];
// ms.sendAsync(address, msgID + " balance",
// BigInteger.valueOf(money[0]).toByteArray()).exceptionally(t -> {
// System.out.println(t.getMessage());
// return null;
// });
// }, es);

// ms.registerHandler("movement", (address, msg) -> {
// String msgID = new String(msg, StandardCharsets.UTF_8).split(" ")[0];
// int movementValue = Integer.parseInt(new String(msg,
// StandardCharsets.UTF_8).split(" ")[1]);

// boolean result = false;
// if (money[0] + movementValue >= 0) {
// result = true;
// money[0] += movementValue;
// }

// byte[] resultBytes = new byte[] { (byte) (result ? 1 : 0) };
// ms.sendAsync(address, msgID + " movement", resultBytes).exceptionally(t -> {
// System.out.println(t.getMessage());
// return null;
// });
// }, es);

// int[] counter = { 0 };
// es.scheduleAtFixedRate(() -> {
// System.out.println(counter[0] + ": Server " + port + " has " + money[0] +
// "€");
// counter[0] += 1;
// }, 0, 2, TimeUnit.SECONDS);
// }
// }
