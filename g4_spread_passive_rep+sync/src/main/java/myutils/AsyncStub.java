package myutils;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;

public class AsyncStub {
  int port;
  final int serverport = 5000;
  ScheduledExecutorService es;
  NettyMessagingService ms;
  ArrayList<String> msgList;

  public AsyncStub(final int port) {
    this.es = Executors.newScheduledThreadPool(1);
    this.ms = new NettyMessagingService("bank", Address.from(port), new MessagingConfig());
    this.msgList = new ArrayList<String>();

    try {
      Thread.sleep(100, 0);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    ms.start();
  }

  public final CompletableFuture<byte[]> sendAndReceiveAsync(final int number, String operation, int accountMovement) {

    CompletableFuture<byte[]> res = new CompletableFuture<byte[]>();

    String customType = port + number + " " + operation;
    byte[] operationMsg = (port + number + " " + accountMovement).getBytes();

    Boolean[] active = { true };
    ms.registerHandler(customType, (address, msg) -> {
      synchronized (active) {
        if (active[0]) {
          active[0] = false;
          ms.unregisterHandler(operation);
          res.complete(msg);
        }
      }
    }, es);

    msgList.add(customType);
    ms.sendAsync(Address.from("localhost", this.serverport), operation, operationMsg);

    return res;
  }
}
