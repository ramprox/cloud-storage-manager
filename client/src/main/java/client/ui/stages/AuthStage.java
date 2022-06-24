package client.ui.stages;

import client.ui.controllers.AuthController;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

/**
 * Окно аутентификации и регистрации
 */
public class AuthStage extends Stage {

    private final AuthController controller;

    public ButtonType getDialogResult() {
        return controller.getDialogResult();
    }

    public String getLogin() {
        return controller.getLogin();
    }

    public String getPassword() {
        return controller.getPassword();
    }

    public AuthStage(AuthController controller) {
        this.controller = controller;
    }
}
