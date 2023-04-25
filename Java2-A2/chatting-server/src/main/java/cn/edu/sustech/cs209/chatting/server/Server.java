package cn.edu.sustech.cs209.chatting.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;


public class Server {

  private ServerSocket serverSocket;
  private List<Handle> clients = new ArrayList<>();
  private List<ChatGroup> groups = new ArrayList<>();

  public Server(int port) {
    try {
      serverSocket = new ServerSocket(port);
      System.out.println("Server started: " + serverSocket);
      while (true) {
        Socket socket = serverSocket.accept();
        System.out.println("Client connected: " + socket);
        Handle handler = new Handle(this, socket);
        clients.add(handler);
        handler.start();
      }
    } catch (IOException ex) {
//            System.out.println("Server exception: " + ex.getMessage());
      ex.printStackTrace();
    }
  }

  public static void main(String[] args) {
    int port = 8080;
    Server server = new Server(port);
  }

  public String getlist(Handle from) {
    String a = "";
    for (Handle client : clients) {
      if (client != from) {
        a = a + client.getname() + "&";
      }
    }
    return a;
  }

  public List<Handle> getallClient() {
    return clients;
  }

  public boolean isinitialized(Handle from, String name) {
    for (Handle client : clients) {
      if ((client.getname().equals(name) && client != from) || from.getname().equals("")) {
        return false;
      }
    }
    return true;
  }

  public void removeClient(Handle handler) {

    clients.remove(handler);
  }

  public Handle getClientByName(String name) {
    for (Handle client : clients) {
      if (client.getname().equals(name)) {
        return client;
      }
    }
    return null;
  }

  public ChatGroup getGroupByName(String name) {
    for (ChatGroup group : groups) {
      if (group.getname().equals(name)) {
        return group;
      }
    }
    return null;
  }

  public void createGroup(String groupName) {
    ChatGroup group = new ChatGroup(groupName);
    boolean flag = true;
    for (ChatGroup a : groups) {
      if (a.getname().equals(groupName)) {
        flag = false;
      }
    }
    if (flag) {
      groups.add(group);
    }
  }

}

class ChatGroup {

  private String name;

  public ChatGroup(String name) {
    this.name = name;
  }

  public String getname() {
    return name;
  }

}

class Handle extends Thread {

  private Server server;
  private BufferedReader in;
  private PrintWriter out;
  private String name = "";
  private boolean initialized = false;


  public Handle(Server server, Socket socket) {
    try {
      this.server = server;
      InputStream inputStream = socket.getInputStream();
      InputStreamReader reader = new InputStreamReader(inputStream, "UTF-8");
      in = new BufferedReader(reader);
      OutputStream outputStream = socket.getOutputStream();
      OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
      out = new PrintWriter(writer, true);
//            name = socket.getInetAddress().getHostAddress();
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  public String getname() {
    return name;
  }


  public void send(Handle handle, String message) {
    handle.out.println(message);
  }

  public void run() {
    try {
      // Read the client's name from the input stream and update the name.
      while (!initialized) {
        String message = in.readLine();
        if (message.startsWith("initialized:")) {
          name = message.split(":")[1];
          boolean flag = server.isinitialized(this, name);
          out.println(flag);

          if (!flag) {
            name = "";
          } else {
            initialized = true;
            String a = "";
            for (int i = 0; i < server.getallClient().size(); i++) {
              a += server.getallClient().get(i).getname() + "&";
            }
            System.out.println("a:" + a);
            for (int i = 0; i < server.getallClient().size(); i++) {
              server.getallClient().get(i).out.println("/denglu/" + a);
            }
          }
        } else {
          out.println("false");
        }
      }

      while (true) {
        String message = in.readLine();
        if (message == null) {
          continue;
        }
        if (message.equals("/list/")) {
          String members = server.getlist(this);
          out.println("/list/" + members);

        } else if (message.startsWith("/leave/")) {
          String leave_name = message.split("/leave/")[1];
          System.out.println("User leave:" + leave_name);
          server.removeClient(this);
          String a = "";
          for (int i = 0; i < server.getallClient().size(); i++) {
            a += server.getallClient().get(i).getname() + "&";
          }
          for (int i = 0; i < server.getallClient().size(); i++) {
            server.getallClient().get(i).out.println("/leave/" + leave_name + "phw" + a);
          }
        } else if (message.startsWith("/txt/")) {
          System.out.println("New Message!");
          System.out.println(message);
          String[] txt = message.split("/txt/")[1].split("phw");
          String to = txt[0];
          String qiehuan = "";
          if (txt.length == 1) {
            qiehuan = "\n";
          } else {
            qiehuan = txt[1] + "\n";
          }
          String ans = "/txt/" + name + "phw" + qiehuan;
          if (server.getClientByName(to) != null) {
            server.getClientByName(to).out.println(ans);
          } else {

            String filename = "chatting-client/" + to + "+" + name + ".txt";
            File file = new File(filename);
            try {
              if (file.exists()) {
                FileOutputStream fos = new FileOutputStream(file, true);
                OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
                osw.append(qiehuan);
                osw.close();
              } else {
                FileOutputStream fos = new FileOutputStream(file);
                OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
                osw.write(qiehuan);
                osw.close();
              }
            } catch (Exception ez) {
              ez.printStackTrace();
            }
          }
        } else if (message.startsWith("/group/")) {
          System.out.println(message);
          String groupname = message.split("/group/")[1];
          server.createGroup(groupname);
          System.out.println("name of groups:" + groupname);
        } else if (message.startsWith("/txt2/")) {
          String sender = message.split("phw")[1];
          System.out.println(sender );
          System.out.println(message);
          String data = message.split("phw")[0];
          String groupname = data.split("/txt2/")[1];
          System.out.println(data);
          System.out.println(groupname);
          String[] menbers = groupname.split("&");

          for (String member : menbers) {
            System.out.println("Member :" + member);
            if (!member.equals(name) && !member.equals("")) {
              System.out.print(member);
              if (server.getClientByName(member) != null) {
                server.getClientByName(member).out.println(message);
              }
            }

          }
        }
        if (message.equals("Dwadwadwadwad")) {
          break;
        }
      }

      server.removeClient(this);
      in.close();
      out.close();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
