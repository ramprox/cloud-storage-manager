package client.controllers;

import client.stages.ConnectionStage;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/**
 * Контроллер окна аутентификации и регистрации
 */
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

    /**
     * Происходит при нажатии кнопки "Sign In" (аутентификации) в окне
     * @param actionEvent событие нажатия кнопки мыши
     */
    public void signInClickAction(ActionEvent actionEvent) {
        Button btn = (Button) actionEvent.getSource();
        ConnectionStage stage = (ConnectionStage)btn.getScene().getWindow();
        stage.setDialogResult(ButtonType.OK);
        stage.close();
    }

    /**
     * Происходит при отмене аутентификации или регистрации
     * @param actionEvent событие нажатия кнопки мыши
     */
    public void cancelClickAction(ActionEvent actionEvent) {
        Button btn = (Button) actionEvent.getSource();
        ConnectionStage stage = (ConnectionStage)btn.getScene().getWindow();
        stage.setDialogResult(ButtonType.CANCEL);
        stage.close();
    }

    /**
     * Происходит при нажатии кнопки "Sign Up" (регистрации) в окне
     * @param actionEvent событие нажатия кнопки мыши
     */
    public void signUpClickAction(ActionEvent actionEvent) {
        Button btn = (Button) actionEvent.getSource();
        ConnectionStage stage = (ConnectionStage)btn.getScene().getWindow();
        stage.setDialogResult(ButtonType.APPLY);
        stage.close();
    }
}
