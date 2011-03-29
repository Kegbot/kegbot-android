/**
 * 
 */
package org.kegbot.core.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * 
 * @author mike
 */
public class KegnetServer {

  private final String mHost;
  private final int mPort;
  private ServerSocket mServerSocket;

  public KegnetServer(String host, int port) {
    mHost = host;
    mPort = port;
  }

  public synchronized void bind() throws UnknownHostException, IOException {
    if (mServerSocket != null) {
      throw new IllegalStateException("Socket is already bound.");
    }
    mServerSocket = new ServerSocket();
    mServerSocket.bind(new InetSocketAddress(mHost, mPort));
  }

  public synchronized void close() {
    if (mServerSocket != null) {
      try {
        mServerSocket.close();
      } catch (IOException e) {
        // Ignore.
      }
      mServerSocket = null;
    }
  }

  public static KegnetMessage getNextMessage(Socket socket) throws IOException {
    final BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    final String rawMessage = reader.readLine();
    if (rawMessage == null) {
      return null;
    }
    if (reader.read() != '\n') {
      return null;
    }

    final ObjectMapper mapper = new ObjectMapper();
    final JsonNode root = mapper.readValue(rawMessage, JsonNode.class);
    return new KegnetMessage(root.get("event").getTextValue(), root.get("data"));
  }

  public Socket accept() throws IOException {
    return mServerSocket.accept();
  }

  public static void main(String args[]) {
    KegnetServer c = new KegnetServer("0.0.0.0", 9805);
    try {
      c.bind();
      System.out.println("Server bound");
      final Socket s = c.accept();
      System.out.println("New client: " + s.getRemoteSocketAddress());
      while (true) {
        System.out.println(KegnetServer.getNextMessage(s) + "\n");
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
