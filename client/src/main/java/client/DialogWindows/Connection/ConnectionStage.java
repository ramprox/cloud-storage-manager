package client.DialogWindows.Connection;

import client.MainClientApp;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class ConnectionStage extends Stage {

    private ConnectionController controller;
    private ButtonType dialogResult = ButtonType.CANCEL;

    public ButtonType getDialogResult() {
        return dialogResult;
    }

    public void setDialogResult(ButtonType dialogResult) {
        this.dialogResult = dialogResult;
    }

    public String getIPAddress() {
        return controller.getIPAddress();
    }

    public int getPort() {
        return controller.getPort();
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
        initModality(Modality.APPLICATION_MODAL);
    }
}
