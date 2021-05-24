package client.DialogWindows.Connection;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class ConnectionController {
    @FXML
    private TextField ipAddress;
    @FXML
    private TextField port;
    @FXML
    private TextField login;
    @FXML
    private PasswordField password;

    private int portValue;

    public String getIPAddress() {
        return ipAddress.getText();
    }

    public int getPort() {
        return portValue;
    }

    public String getLogin() {
        return login.getText();
    }

    public String getPassword() {
        return password.getText();
    }

    public void okClickAction(ActionEvent actionEvent) {
        try {
            portValue = Integer.parseInt(port.getText());
        } catch (NumberFormatException ex) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.showAndWait();
        }
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
}
