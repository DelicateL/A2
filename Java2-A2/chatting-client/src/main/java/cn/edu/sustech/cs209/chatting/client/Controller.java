package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import java.net.InetAddress;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.time.*;

public class Controller implements Initializable {

  @FXML
  ListView<Message> chatContentList;
  @FXML
  ListView<String> chatList;
  @FXML
  Label currentUsername;
  @FXML
  Label currentOnlineCnt;
  List<Stage> window;
  List<Stage> GroupWindow;
  String username;
  String userList = "";
  private boolean initialized = false;
  private Socket socket;
  private BufferedReader in;
  private PrintWriter out;
  private Stage bigStage;

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
    chatContentList.setCellFactory(new MessageCellFactory());

    ObservableList<String> items = FXCollections.observableArrayList();
    chatList.setItems(items);
    try {
      window = new ArrayList<>();
      GroupWindow = new ArrayList<>();
      socket = new Socket(InetAddress.getLocalHost(), 8080);
      InputStream inputStream = socket.getInputStream();
      InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
      in = new BufferedReader(reader);
      OutputStream outputStream = socket.getOutputStream();
      OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
      out = new PrintWriter(writer, true);
      while (!initialized) {
        TextInputDialog errorDialog = new TextInputDialog();
        errorDialog.setTitle("login");
        errorDialog.setHeaderText(null);
        errorDialog.setContentText("Account:");
        Optional<String> input = errorDialog.showAndWait();
        if (!input.isPresent() || input.get().isEmpty()) {
          continue;
        } else {
          username = input.get();
          currentUsername.setText(username);
        }
        out.println("initialized:" + username);
        initialized = Boolean.parseBoolean(in.readLine());
        if (initialized) {
          break;
        }

      }
      out.println("/list/");
      Thread clientListener = new Thread(() -> {
        boolean flag = true;
        while (flag) {
          try {
            String message = in.readLine();
            bigStage = Client.getBigStage();
            if (bigStage != null) {
              if (!bigStage.isShowing()) {
                out.println("/leave/" + username);
                in.close();
                out.close();
                socket.close();
                Thread.currentThread().interrupt();
              }
            }

            if (message.startsWith("/list/")) {
              String a = "";
              String[] users = message.split("/list/");
              if (users.length > 1) {
                users = users[1].split("&");
                for (String u : users) {
                  if (!u.equals(username) && !u.equals("")) {
                    a += u + "&";
                  }
                }
              }
              userList = a;
            } else if (message.startsWith("/denglu/")) {
              String finalA = message;
              System.out.println(userList);
              Platform.runLater(() -> {
                items.clear();
                String list = finalA.split("/denglu/")[1];
                String[] nbs = list.split("&");
//                                System.out.println("length:"+nbs.length);
                currentOnlineCnt.setText("CurrentOnlineUser:" + String.valueOf(nbs.length));
                for (String nb : nbs) {
                  if (!nb.equals("") && !nb.equals(username)) {
                    System.out.println("items:" + nb);
                    items.add(nb);
                  }
                }
              });
            } else if (message.startsWith("/txt/")) {
              message = message.split("/txt/")[1];
              String[] txt = message.split("phw");
              String from = txt[0];
              String qiehuan = "";
              if (txt.length == 1) {
                qiehuan = "\n";
              } else {
                qiehuan = txt[1] + "\n";
              }

              System.out.print(qiehuan);
              String filename = username + "+" + from + ".txt";
              File file = new File(filename);
              String finalQiehuan = qiehuan;
              Platform.runLater(() -> {
                System.out.print(finalQiehuan);
                System.out.print(from + ":\n");
                if (finalQiehuan.split(":")[0].equals((from + ":\n").split(":")[0])) {

                  Message a = new Message(System.currentTimeMillis(), txt[0], username,
                      "send to you new message");
                  chatContentList.getItems().add(a);
                }
                for (Stage i : window) {
                  if (i.getTitle().equals(from)) {
                    Scene scene = (Scene) i.getScene();
                    VBox vbox = (VBox) scene.getRoot();
                    TextArea chartArea = (TextArea) vbox.getChildren().get(0);
                    chartArea.appendText(finalQiehuan);
                  }
                }
              });
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
            } else if (message.startsWith("/leave/")) {
              String leave_name = message.split("/leave/")[1].split("phw")[0];
              System.out.println("leave_name:" + leave_name);
              Platform.runLater(() -> {
                Message a = new Message(System.currentTimeMillis(), leave_name, username,
                    "log out");
                chatContentList.getItems().add(a);
              });
              String finalA = leave_name;
              String finalB = message.split("/leave/")[1].split("phw")[1];
              Platform.runLater(() -> {
                items.clear();
                String[] nbs = finalB.split("&");
                for (String nb : nbs) {
                  if (!nb.equals("") && !nb.equals(username)) {
                    items.add(nb);
                  }
                }
                System.out.println("nb2");
              });
              for (Stage qunliao : GroupWindow) {
                String[] zaixian = finalB.split("&");
                String[] members = qunliao.getTitle().split("&");
                Scene scene = (Scene) qunliao.getScene();
                HBox hbox = (HBox) scene.getRoot();
                TextArea listArea = (TextArea) hbox.getChildren().get(0);
                listArea.clear();
                for (String x : zaixian) {
                  for (String y : members) {
                    if (x.equals(y) && !y.equals(username)) {
                      listArea.appendText(y + "\n");
                    }
                  }
                }
              }

            } else if (message.startsWith("/txt2/")) {
              String groupname = message.split("/txt2/")[1].split("phw")[0];
              String sender = message.split("phw")[1];
              Platform.runLater(() -> {
                Message a = new Message(System.currentTimeMillis(), sender, username,
                    groupname + "有新消息");
                chatContentList.getItems().add(a);
                for (Stage i : GroupWindow) {
                  if (i.getTitle().equals(groupname)) {
                    Scene scene = (Scene) i.getScene();
                    HBox hbox = (HBox) scene.getRoot();
                    VBox vbox = (VBox) hbox.getChildren().get(1);
                    TextArea chatArea = (TextArea) vbox.getChildren().get(0);
                    try {
                      System.out.println("shoudaoxiaoxi");
                      String filename = groupname + ".txt";
                      File file = new File(filename);
                      FileInputStream fis = new FileInputStream(file);
                      InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
                      BufferedReader br = new BufferedReader(isr);
                      StringBuilder sb = new StringBuilder();
                      String line;
                      while ((line = br.readLine()) != null) {
                        sb.append(line + "\n");
                      }
                      br.close();
                      String content = sb.toString();
                      chatArea.clear();
                      chatArea.appendText(content);
                    } catch (Exception e) {

                    }
                  }
                }
              });

            }
            out.println("/list/");

          } catch (Exception e) {
            flag = false;
            if (e.toString().contains("Connection reset")) {
              Platform.runLater(() -> {
                for (Stage s : window) {
                  s.close();
                }
                for (Stage s : GroupWindow) {
                  s.close();
                }
                assert bigStage != null;
                bigStage.close();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText(null);
                alert.setContentText("Server disconnected");

                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                  Platform.exit();
                  System.exit(0);
                }
              });
            }

          }
        }
      });
      clientListener.start();
    } catch (IOException e) {
      TextInputDialog errorDialog = new TextInputDialog();
      errorDialog.setTitle("Error");
      errorDialog.setHeaderText(null);
      errorDialog.setContentText("Con't connect to server, please try again");
      errorDialog.showAndWait();
      System.exit(0);
    }

  }

  @FXML
  public synchronized void createPrivateChat() {
    AtomicReference<String> user = new AtomicReference<>();
    Stage stage = new Stage();
    ComboBox<String> userSel = new ComboBox<>();

    String[] users = userList.split("&");
    for (String u : users) {
      if (!u.equals(username) && !u.equals("")) {
        userSel.getItems().add(u);
      }
    }

    // FIXME: get the user list from server, the current user's name should be filtered ou
    Button okBtn = new Button("Go PrivateChat");
    okBtn.setOnAction(e -> {
      if (userSel.getSelectionModel().getSelectedItem() != null) {
        System.out.print(userSel.getSelectionModel().getSelectedItem());
        stage.close();
        user.set(userSel.getSelectionModel().getSelectedItem());
        boolean flag = true;
        for (Stage i : window) {
          if (i.getTitle().equals(user.get())) {
            flag = false;
          }
        }
        if (flag) {
          Stage stage2 = new Stage();
          window.add(stage2);
          stage2.setOnCloseRequest(event -> {
            window.remove(stage2);
          });
          stage2.setTitle(user.get());
          TextArea chatArea = new TextArea();
          TextArea inputField = new TextArea();

          String filename = username + "+" + user.get() + ".txt";
          File file = new File(filename);

          try {
            if (file.exists()) {
              FileInputStream fis = new FileInputStream(file);
              InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
              BufferedReader br = new BufferedReader(isr);
              StringBuilder sb = new StringBuilder();
              String line;
              while ((line = br.readLine()) != null) {
                sb.append(line + "\n");
              }
              br.close();
              String content = sb.toString();
              chatArea.clear();
              chatArea.appendText(content);
            } else {
              file.createNewFile();
            }
          } catch (Exception ex) {
            ex.printStackTrace();
          }

          Button sendButton = new Button("Send");
          sendButton.setOnAction(actionEvent -> {
            String message = inputField.getText().trim();
            if (!message.isEmpty()) {
              message = username + ": \n" + message + "\n";
              chatArea.appendText(message);
              inputField.clear();
              String[] ans = message.split("\n");
              for (String a : ans) {
                out.println("/txt/" + user.get() + "phw" + a);
              }
              out.flush();
              try {
                if (file.exists()) {
                  FileOutputStream fos = new FileOutputStream(file, true);
                  OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
                  osw.append(message);
                  osw.close();
                } else {
                  FileOutputStream fos = new FileOutputStream(file);
                  OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
                  osw.write(message);
                  osw.close();
                }
              } catch (Exception ez) {
                ez.printStackTrace();
              }
            }
          });
          Button emojiButton = new Button("emoji"); //
          emojiButton.setOnAction(actionEvent -> {
            String message = inputField.getText().trim();
            if (!message.isEmpty()) {
              inputField.appendText(" \uD83D\uDE00"); // 在消息后追加笑脸 emoji
            }
          });

          HBox inputBox = new HBox(inputField, sendButton, emojiButton);
          inputBox.setSpacing(10);

          VBox root = new VBox();
          root.getChildren().addAll(chatArea, inputBox);

          Scene scene = new Scene(root, 400, 400);
          stage2.setScene(scene);
          stage2.show();

        }
      }
    });

    HBox box = new HBox(10);
    box.setAlignment(Pos.CENTER);
    box.setPadding(new Insets(20, 20, 20, 20));
    box.getChildren().addAll(userSel, okBtn);
    stage.setScene(new Scene(box));
    stage.showAndWait();
  }

  /**
   * A new dialog should contain a multi-select list, showing all user's name. You can select
   * several users that will be joined in the group chat, including yourself.
   * <p>
   * The naming rule for group chats is similar to WeChat: If there are > 3 users: display the first
   * three usernames, sorted in lexicographic order, then use ellipsis with the number of users, for
   * example: UserA, UserB, UserC... (10) If there are <= 3 users: do not display the ellipsis, for
   * example: UserA, UserB (2)
   */
  @FXML
  public void createGroupChat() {
//        AtomicReference<String> selectedGroup = new AtomicReference<>();
//        Stage stage = new Stage();

    AtomicReference<String> selectedUsers = new AtomicReference<>("");
    Stage selectedStage = new Stage();
    VBox duoxuan = new VBox();//用来放多选框的
    //得到用户列表，注册checkbox 函数
    String[] users = userList.split("&");
    for (int i = 0; i < users.length; i++) {
      if (!users[i].equals(username) && !users[i].equals("")) {
        CheckBox a = new CheckBox(users[i]);
        a.selectedProperty().addListener((observableValue, oldValue, newValue) -> {
          if (newValue) {
            if (selectedUsers.get().equals("")) {
              selectedUsers.set(a.getText() + "&");
            } else {
              selectedUsers.set(selectedUsers.get() + a.getText() + "&");
            }
          } else {
            String temp = selectedUsers.get().replace(a.getText() + "&", "");
            selectedUsers.set(temp);

          }
        });
        duoxuan.getChildren().add(a);
      }
    }
    Button selectedBtn = new Button("Go GroupChat!");
    selectedBtn.setOnAction(e -> {
      if (!selectedUsers.get().isEmpty() && !selectedUsers.get().equals("")) {
        selectedStage.close();

        String an = selectedUsers.get();
        System.out.println("selected userlist:" + an);
        an += username;
        System.out.println("add me:" + an);
        String[] anss = an.split("&");
        Arrays.sort(anss);
        String names = "";
        for (String ans : anss) {
          names += ans + "&";
        }
        System.out.println("realname:" + names);
        boolean flag = true;
        for (Stage i : GroupWindow) {
          System.out.println("a");
          if (i.getTitle().equals(names)) {
            flag = false;
          }
        }
        if (flag) {
          out.println("/group/" + names);
          Stage stage2 = new Stage();
          GroupWindow.add(stage2);
          stage2.setOnCloseRequest(event -> {
          GroupWindow.remove(stage2);
          });
          stage2.setTitle(names);
          TextArea listArea = new TextArea();
          TextArea chatArea = new TextArea();
          TextArea inputField = new TextArea();

          listArea.clear();
          String[] nbs = selectedUsers.get().split("&");
          for (String nb : nbs) {
            if (!nb.equals("")) {
              listArea.appendText(nb + "\n");
            }
          }

          String filename = names + ".txt";
          File file = new File(filename);
          try {
            if (file.exists()) {
              FileInputStream fis = new FileInputStream(file);
              InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
              BufferedReader br = new BufferedReader(isr);
              StringBuilder sb = new StringBuilder();
              String line;
              while ((line = br.readLine()) != null) {
                sb.append(line + "\n");
              }
              br.close();
              String content = sb.toString();
              chatArea.clear();
              chatArea.appendText(content);
            } else {
              file.createNewFile();
            }
          } catch (Exception ex) {
            ex.printStackTrace();
          }

          Button sendButton = new Button("Send");
          String finalNames = names;
          sendButton.setOnAction(actionEvent -> {
            String message = inputField.getText().trim();
            if (!message.isEmpty()) {
              message = username + ": \n" + message + "\n";
              chatArea.appendText(message);
              inputField.clear();
              String[] ans = message.split("\n");
              out.println("/txt2/" + finalNames + "phw" + username);

              out.flush();
              try {
                if (file.exists()) {
                  FileOutputStream fos = new FileOutputStream(file, true);
                  OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
                  osw.append(message);
                  osw.close();
                } else {
                  FileOutputStream fos = new FileOutputStream(file);
                  OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
                  osw.write(message);
                  osw.close();
                }
              } catch (Exception ez) {
                ez.printStackTrace();
              }
            }
          });

          Button emojiButton = new Button("emoji"); //
          emojiButton.setOnAction(actionEvent -> {
            String message = inputField.getText().trim();
            if (!message.isEmpty()) {
              inputField.appendText(" \uD83D\uDE0D");
            }
          });

          HBox inputBox = new HBox(inputField, sendButton, emojiButton);
          inputBox.setSpacing(10);

          VBox root2 = new VBox();
          root2.getChildren().addAll(chatArea, inputBox);

          HBox root = new HBox();
          root.getChildren().addAll(listArea, root2);

          Scene scene = new Scene(root, 600, 600);
          stage2.setScene(scene);
          stage2.show();

        }
      }
    });

    HBox box = new HBox();
    box.getChildren().addAll(duoxuan, selectedBtn);
    selectedStage.setScene(new Scene(box));
    selectedStage.show();
  }

  /**
   * Sends the message to the <b>currently selected</b> chat.
   * <p>
   * Blank messages are not allowed. After sending the message, you should clear the text input
   * field.
   */
  @FXML
  public void doSendMessage() {
    // TODO
  }

  /**
   * You may change the cell factory if you changed the design of {@code Message} model. Hint: you
   * may also define a cell factory for the chats displayed in the left panel, or simply override
   * the toString method.
   */
  private class MessageCellFactory implements Callback<ListView<Message>, ListCell<Message>> {

    @Override
    public ListCell<Message> call(ListView<Message> param) {
      return new ListCell<Message>() {

        @Override
        public void updateItem(Message msg, boolean empty) {
          super.updateItem(msg, empty);
          if (empty || Objects.isNull(msg)) {
            return;
          }

          HBox wrapper = new HBox();
          Label nameLabel = new Label(msg.getSentBy());
          Label msgLabel = new Label(msg.getData());
          Instant instant = Instant.ofEpochMilli(msg.getTimestamp());
          ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
          String formattedDateTime = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(zonedDateTime);
          Label timeLabel = new Label(formattedDateTime);

          nameLabel.setPrefSize(50, 20);
          nameLabel.setWrapText(true);
          nameLabel.setStyle("-fx-border-color: black; -fx-border-width: 1px;");

          wrapper.setAlignment(Pos.TOP_LEFT);
          wrapper.getChildren().addAll(timeLabel, nameLabel, msgLabel);
          msgLabel.setPadding(new Insets(0, 20, 0, 0));

          setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
          setGraphic(wrapper);
        }
      };
    }
  }


}