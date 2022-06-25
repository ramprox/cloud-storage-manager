package client.ui.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.text.NumberFormat;

/**
 * Контроллер окна, в котором отображается ход загрузки или скачивания (upload и download)
 */
public class ProgressController {
    @FXML private Label message;
    @FXML private ProgressBar progress;
    @FXML private Label percent;
    @FXML private Label fileName;

    private static final NumberFormat percentFormatter = NumberFormat.getPercentInstance();

    public void setMessage(String text) {
        message.setText(text);
    }

    public void setProgress(double value) {
        progress.setProgress(value);
        percent.setText(percentFormatter.format(value));
    }

    public void setFileName(String fileName) {
        this.fileName.setText(fileName);
    }
}
