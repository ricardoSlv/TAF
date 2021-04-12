package myutils;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import spread.BasicMessageListener;
import spread.SpreadConnection;
import spread.SpreadException;
import spread.SpreadMessage;

public class AsyncStub {
  Map<Integer, CompletableFuture<String>> requestAnswers;
  SpreadConnection conn;

  public AsyncStub(final int clientID) {
    this.requestAnswers = new HashMap<Integer, CompletableFuture<String>>();
    this.conn = new SpreadConnection();
    try {
      conn.connect(InetAddress.getByName("localhost"), 4803, "client" + clientID, false, false);
    } catch (Exception e) {
      e.printStackTrace();
    }

    conn.add(new BasicMessageListener() {
      @Override
      public void messageReceived(SpreadMessage msg) {
        byte[] data = msg.getData();
        String msgString = new String(data, StandardCharsets.UTF_8);
        String[] messageBits = msgString.split(" ");
        int reqId = Integer.parseInt(messageBits[0]);
        requestAnswers.get(reqId).complete(messageBits[1]);
      }
    });
  }

  public final CompletableFuture<String> sendAndReceiveAsync(final int number, String operation, int accountMovement) {

    CompletableFuture<String> res = new CompletableFuture<String>();
    requestAnswers.put(number, res);

    byte[] operationMsg = (number + " " + operation + " " + accountMovement).getBytes();

    SpreadMessage msg = new SpreadMessage();
    msg.setData(operationMsg);
    msg.setSafe();
    msg.addGroup("servers");

    try {
      conn.multicast(msg);
    } catch (SpreadException e) {
      e.printStackTrace();
    }

    return res;
  }
}
