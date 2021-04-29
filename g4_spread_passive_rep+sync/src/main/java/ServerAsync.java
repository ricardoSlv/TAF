import java.math.BigInteger;
import java.net.InetAddress;
import java.nio.ByteBuffer;
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

  public static void main(String[] args) {

    int serverId = Integer.parseInt(args[0]);
    ScheduledExecutorService es = Executors.newScheduledThreadPool(1);
    NettyMessagingService ms = new NettyMessagingService("bank", Address.from(5000), new MessagingConfig());

    int[] money = { 1000 };
    boolean[] active = { false };
    ArrayList<SpreadMessage> delayedMessages = new ArrayList<>();
    SpreadConnection conn = new SpreadConnection();

    byte[][] lastMsg = { null };
    Address[] lastMsgAddress = { null };

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
        if (imTheleader[0]) {
          // Receiving my own safe message means every member os the group has already received it also
          String[] msgParts = new String(lastMsg[0], StandardCharsets.UTF_8).split(" ");
          String msgID = msgParts[0];
          ms.sendAsync(lastMsgAddress[0], msgID + " echo", Integer.toString(money[0]).getBytes()).exceptionally(t -> {
            System.out.println(t.getMessage());
            return null;
          });
          System.out.println("Answered the client " + money[0]);

        } else {
          money[0] = new BigInteger(msg.getData()).intValue();
          System.out.println("Got new state " + money[0]);
        }
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
              lastMsg[0] = msge;
              lastMsgAddress[0] = address;
              SpreadMessage rep = new SpreadMessage();
              money[0]++;
              byte[] resp = BigInteger.valueOf(money[0]).toByteArray();
              rep.setData(resp);
              rep.setReliable();
              rep.setSafe();
              rep.addGroup("servers");
              try {
                conn.multicast(rep);
              } catch (SpreadException e) {
                e.printStackTrace();
              }

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
      counter[0] += 1;
    }, 0, 2, TimeUnit.SECONDS);
  }
}