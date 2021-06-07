package client.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;

/**
 * Класс окна, в котором отображается ход загрузки или скачивания (upload и download)
 */
public class ProgressController {
    @FXML
    private Label message;
    @FXML
    private ProgressBar progress;

    public void setMessage(String text) {
        message.setText(text);
    }

    public void setProgress(double value) {
        progress.setProgress(value);
    }
}
