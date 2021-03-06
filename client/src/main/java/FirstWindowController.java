import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class FirstWindowController implements Initializable {
    @FXML
    VBox autorization;
    @FXML
    TextField loginField;
    @FXML
    PasswordField passwordField;
    @FXML
    VBox registration;
    @FXML
    Label messageToUser;
    @FXML
    Label messageToUserRegistration;
    @FXML
    TextField loginFieldReg;
    @FXML
    PasswordField passwordField1;
    @FXML
    PasswordField passwordField2;
    @FXML
    Button but0;
    @FXML
    HBox authHbox;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        ConnectWithServer.connect();

        Thread ServerListener = new Thread(() -> {
            while (true){
                Object messageFromServer = null;
                try {
                    messageFromServer = ConnectWithServer.readInObject();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                if (messageFromServer instanceof CommandMessage) {
                    switch (((CommandMessage) messageFromServer).getCommand()) {
                        case AUTH_SUCCESS:
                            UserLogin.setLogin(((CommandMessage) messageFromServer).getMsg());
                            SuccessfulEnter();
                            break;
                        case AUTH_FAILED:
                            Platform.runLater(() -> messageToUser.setText("Wrong password"));
                            break;
                        case AUTH_NO_SUCH_USER:
                            Platform.runLater(() -> messageToUser.setText("Such user doesn't exist"));
                            break;
                        case REG_ALREADY_EXIST:
                            Platform.runLater(() -> {
                                messageToUserRegistration.setText("Such user already exists");
                                loginFieldReg.clear();
                                passwordField1.clear();
                                passwordField2.clear();
                            });
                            break;
                        case REG_SUCCESS:
                            Object finalMessageFromServer = messageFromServer;
                            Platform.runLater(() -> {
                                exitReg();
                                messageToUser.setText("Registration is successful. Enter in your account");
                                loginField.clear();
                                loginField.insertText(0,((CommandMessage) finalMessageFromServer).getMsg());
                            });
                            break;
                    }

                }
            }
        });

        ServerListener.setDaemon(true);
        ServerListener.start();
    }

    public void showRegForm(){
        autorization.setVisible(false);
        authHbox.setVisible(false);
        registration.setVisible(true);
    }
    public void exitReg(){
        autorization.setVisible(true);
        authHbox.setVisible(true);
        registration.setVisible(false);
    }
    public void sendAuthMessage(){
        if (!loginField.getText().isEmpty() && !passwordField.getText().isEmpty()){
            ConnectWithServer.sendAuthMessage(loginField.getText(),passwordField.getText());
            loginField.clear();
            passwordField.clear();
        }
    }
    public void sendRegMessage(){
        if (!loginFieldReg.getText().isEmpty() && !passwordField1.getText().isEmpty() && !passwordField2.getText().isEmpty()){
            if (passwordField1.getText().equals(passwordField2.getText())){
                ConnectWithServer.sendRegMessage(loginFieldReg.getText(),passwordField1.getText());
            }else {
                messageToUserRegistration.setText("Make sure the passwords match");
                passwordField1.clear();
                passwordField2.clear();
            }
        }
    }

    public void switchScene() throws IOException {
        Stage stage;
        Parent root;
        stage = (Stage)but0.getScene().getWindow();
        root = FXMLLoader.load(getClass().getResource("/MainWindow.fxml"));
        Image icon = new Image(getClass().getResourceAsStream("/iconfinder_cloud_1287533.png"));
        stage.getIcons().add(icon);
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.setTitle("Cloud_core");

        stage.show();
    }
    public void SuccessfulEnter(){
        Platform.runLater(() -> {
            try {
                switchScene();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }



}
