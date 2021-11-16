package client.ui.stages;

import client.ui.MainClientApp;
import client.ui.controllers.ConnectionController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.io.IOException;

/**
 * Окно аутентификации и регистрации
 */
public class ConnectionStage extends Stage {

    private final ConnectionController controller;
    private ButtonType dialogResult = ButtonType.CANCEL;

    public ButtonType getDialogResult() {
        return dialogResult;
    }

    public void setDialogResult(ButtonType dialogResult) {
        this.dialogResult = dialogResult;
    }

    public String getLogin() {
        return controller.getLogin();
    }

    public String getPassword() {
        return controller.getPassword();
    }

    public ConnectionStage() throws IOException {
        setTitle("Connection");
        FXMLLoader loader = new FXMLLoader(MainClientApp.class.getResource("/windows/Connection.fxml"));
        Parent root = loader.load();
        controller = loader.getController();
        Scene scene = new Scene(root);
        setScene(scene);
        setResizable(false);
        sizeToScene();
        initStyle(StageStyle.UTILITY);
        initModality(Modality.APPLICATION_MODAL);
    }

    public void setOwner(Window window) {
        initOwner(window);
    }
}
