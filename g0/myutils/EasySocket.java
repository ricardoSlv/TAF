package myutils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class EasySocket extends Socket {
  private BufferedReader br;
  private PrintWriter pw;

  public EasySocket(String host, int port) throws UnknownHostException, IOException {
    super(host, port);
    this.br = new BufferedReader(new InputStreamReader(this.getInputStream()));
    this.pw = new PrintWriter(new OutputStreamWriter(this.getOutputStream()), true);
  }

  public void println(String s) {
    this.pw.println(s);
  }

  public String readLine() throws IOException {
    return this.br.readLine();
  }
}
