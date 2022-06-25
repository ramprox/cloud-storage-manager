package client.ui.controllers;

import client.ui.stages.AuthStage;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/**
 * Контроллер окна аутентификации и регистрации
 */
public class AuthController {

    @FXML private TextField login;

    @FXML private PasswordField password;

    private ButtonType dialogResult = ButtonType.CANCEL;

    public String getLogin() {
        return login.getText();
    }

    public String getPassword() {
        return password.getText();
    }

    public ButtonType getDialogResult() {
        return dialogResult;
    }

    /**
     * Происходит при нажатии кнопки "Sign In" (аутентификации) в окне
     * @param actionEvent событие нажатия кнопки мыши
     */
    public void signInClickAction(ActionEvent actionEvent) {
        Button btn = (Button) actionEvent.getSource();
        AuthStage stage = (AuthStage)btn.getScene().getWindow();
        this.dialogResult = ButtonType.OK;
        stage.close();
    }

    /**
     * Происходит при отмене аутентификации или регистрации
     * @param actionEvent событие нажатия кнопки мыши
     */
    public void cancelClickAction(ActionEvent actionEvent) {
        Button btn = (Button) actionEvent.getSource();
        AuthStage stage = (AuthStage)btn.getScene().getWindow();
        stage.close();
    }

    /**
     * Происходит при нажатии кнопки "Sign Up" (регистрации) в окне
     * @param actionEvent событие нажатия кнопки мыши
     */
    public void signUpClickAction(ActionEvent actionEvent) {
        Button btn = (Button) actionEvent.getSource();
        AuthStage stage = (AuthStage)btn.getScene().getWindow();
        this.dialogResult = ButtonType.APPLY;
        stage.close();
    }
}
