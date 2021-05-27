package client.DialogWindows.Connection;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class ConnectionController {
    @FXML
    private TextField login;
    @FXML
    private PasswordField password;

    public String getLogin() {
        return login.getText();
    }

    public String getPassword() {
        return password.getText();
    }

    public void signInClickAction(ActionEvent actionEvent) {
        Button btn = (Button) actionEvent.getSource();
        ConnectionStage stage = (ConnectionStage)btn.getScene().getWindow();
        stage.setDialogResult(ButtonType.OK);
        stage.close();
    }

    public void cancelClickAction(ActionEvent actionEvent) {
        Button btn = (Button) actionEvent.getSource();
        ConnectionStage stage = (ConnectionStage)btn.getScene().getWindow();
        stage.setDialogResult(ButtonType.CANCEL);
        stage.close();
    }

    public void signUpClickAction(ActionEvent actionEvent) {
        Button btn = (Button) actionEvent.getSource();
        ConnectionStage stage = (ConnectionStage)btn.getScene().getWindow();
        stage.setDialogResult(ButtonType.APPLY);
        stage.close();
    }
}
