import java.net.InetAddress;
import java.net.UnknownHostException;
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

import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;

class ElectionManager {
  private String currLeader;
  private int maxSeqNumber;
  private ArrayList<String> groupMembers;
  private int completedElements;

  ElectionManager() {
  }

  public void startElection(SpreadGroup[] members) {
    this.currLeader = null;
    this.maxSeqNumber = -1;
    this.groupMembers = new ArrayList<>();
    System.out.println(members);
    // TODO: members is null :^/
    for (SpreadGroup m : members)
      this.groupMembers.add(m.toString());
    this.completedElements = 0;
  }

  public void processElement(SpreadGroup spreadmember, int seqnumber) {

    String member = spreadmember.toString();
    // Wins by seqnumber, solves draws by name
    if (seqnumber > maxSeqNumber || (seqnumber == maxSeqNumber && member.compareTo(currLeader) > 0)) {
      maxSeqNumber = seqnumber;
      currLeader = member;
      completedElements++;
    }
  }

  public Boolean isFinished() {
    return this.completedElements == this.groupMembers.size();
  }

  public String getLeader() {
    return this.currLeader;
  }
}

class SimpleMessage {
  public String topic;
  public String value;

  public SimpleMessage(byte[] bytes) {
    String[] msgParts = (new String(bytes, StandardCharsets.UTF_8)).split(" ");
    this.topic = msgParts[0];
    this.value = msgParts[0];
  }

  public SimpleMessage(String topic, int value) {
    this.topic = topic;
    this.value = Integer.toString(value);
  }

  public int intValue() {
    return Integer.parseInt(this.value);
  }

  public Boolean isElection() {
    return this.topic.equals("election");
  }

  public Boolean isState() {
    return this.topic.equals("state");
  }

  @Override
  public String toString() {
    return topic + " " + value;
  }

  public byte[] toBytes() {
    return this.toString().getBytes();
  }

}

public class ServerAsync {

  private static void sendSpreadSafeMsg(SpreadConnection conn, SimpleMessage msg, String group) {
    SpreadMessage rep = new SpreadMessage();

    byte[] resp = msg.toBytes();
    rep.setData(resp);
    rep.setReliable();
    rep.setSafe();
    rep.addGroup(group);
    try {
      conn.multicast(rep);
    } catch (SpreadException e) {
      e.printStackTrace();
    }
  }

  private static void startClientService(NettyMessagingService ms, ScheduledExecutorService es, SpreadConnection conn,
      int[] seqnumber, byte[][] lastMsg, Address[] lastMsgAddress) {
    ms.start();
    ms.registerHandler("echo", (address, msge) -> {
      lastMsg[0] = msge;
      lastMsgAddress[0] = address;
      seqnumber[0]++;
      byte[] resp = new SimpleMessage("state", seqnumber[0]).toBytes();
      SpreadMessage rep = new SpreadMessage();
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
  }

  private static void sendClientResponse(NettyMessagingService ms, byte[][] lastMsg, Address[] lastMsgAddress,
      int[] seqnumber) {
    String[] msgParts = new String(lastMsg[0], StandardCharsets.UTF_8).split(" ");
    String msgID = msgParts[0];
    ms.sendAsync(lastMsgAddress[0], msgID + " echo", Integer.toString(seqnumber[0]).getBytes()).exceptionally(t -> {
      System.out.println(t.getMessage());
      return null;
    });
    System.out.println("Answered the client " + seqnumber[0]);

  }

  private static void stopClientService(NettyMessagingService ms) {
    ms.unregisterHandler("echo");
    ms.stop();
  }

  public static void main(String[] args) throws InterruptedException, UnknownHostException, SpreadException {

    int totalMembers = 3;
    int serverId = Integer.parseInt(args[0]);
    ScheduledExecutorService es = Executors.newScheduledThreadPool(1);
    NettyMessagingService ms = new NettyMessagingService("bank", Address.from(5000), new MessagingConfig());
    SpreadConnection conn = new SpreadConnection();

    int[] seqnumber = { 1000 };
    byte[][] lastMsg = { null };
    Address[] lastMsgAddress = { null };
    boolean[] imTheleader = { false };
    final ElectionManager elManager = new ElectionManager();

    conn.connect(InetAddress.getByName("localhost"), 4800 + serverId, "server" + serverId, false, true);

    conn.add(new AdvancedMessageListener() {
      @Override
      public void regularMessageReceived(SpreadMessage msg) {
        SimpleMessage readableMsg = new SimpleMessage(msg.getData());

        if (readableMsg.isElection()) {
          elManager.processElement(msg.getSender(), readableMsg.intValue());
          // I won the election
          if (elManager.isFinished() && elManager.getLeader().equals(conn.getPrivateGroup().toString())) {
            imTheleader[0] = true;
            System.out.println("Im the leader " + seqnumber[0]);
            startClientService(ms, es, conn, seqnumber, lastMsg, lastMsgAddress);
          }
        } else if (readableMsg.isState()) {
          if (imTheleader[0]) {
            // Receiving my own safe message means every member os the group has already
            // received it also
            sendClientResponse(ms, lastMsg, lastMsgAddress, seqnumber);
          } else {
            seqnumber[0] = readableMsg.intValue();
            System.out.println("Got new state " + seqnumber[0]);
          }
        }
      }

      @Override
      public void membershipMessageReceived(SpreadMessage msg) {
        MembershipInfo info = msg.getMembershipInfo();
        if (info.isTransition()) {
          elManager.startElection(info.getMembers());

          int electionNumber = imTheleader[0] ? 99999999 : seqnumber[0];
          SimpleMessage electionMsg = new SimpleMessage("election", electionNumber);
          sendSpreadSafeMsg(conn, electionMsg, "servers");

          if (imTheleader[0] && (info.getMembers().length < (totalMembers / 2))) {
            imTheleader[0] = false;
            stopClientService(ms);
            startClientService(ms, es, conn, seqnumber, lastMsg, lastMsgAddress);
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

    // Prevent Thread from dying
    Thread.sleep(Integer.MAX_VALUE);
  }
}