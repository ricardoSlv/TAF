import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ServerAsync {

  public static void main(String[] args) {

    int port = Integer.parseInt(args[0]);
    ScheduledExecutorService es = Executors.newScheduledThreadPool(1);
    NettyMessagingService ms = new NettyMessagingService("bank", Address.from(port), new MessagingConfig());

    try {
      Thread.sleep(100, 0);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    ms.start();
    int[] money = { 1000 };

    ms.registerHandler("balance", (address, msg) -> {
      String msgID = new String(msg, StandardCharsets.UTF_8).split(" ")[0];
      ms.sendAsync(address, msgID + " balance", BigInteger.valueOf(money[0]).toByteArray()).exceptionally(t -> {
        System.out.println(t.getMessage());
        return null;
      });
    }, es);

    ms.registerHandler("movement", (address, msg) -> {
      String msgID = new String(msg, StandardCharsets.UTF_8).split(" ")[0];
      int movementValue = Integer.parseInt(new String(msg, StandardCharsets.UTF_8).split(" ")[1]);

      boolean result = false;
      if (money[0] + movementValue >= 0) {
        result = true;
        money[0] += movementValue;
      }

      byte[] resultBytes = new byte[] { (byte) (result ? 1 : 0) };
      ms.sendAsync(address, msgID + " movement", resultBytes).exceptionally(t -> {
        System.out.println(t.getMessage());
        return null;
      });
    }, es);

    int[] counter = { 0 };
    es.scheduleAtFixedRate(() -> {
      System.out.println(counter[0] + ": Server " + port + " has " + money[0] + "€");
      counter[0] += 1;
    }, 0, 2, TimeUnit.SECONDS);
  }
}
