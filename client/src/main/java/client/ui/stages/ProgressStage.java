package client.ui.stages;

import client.ui.controllers.ProgressController;
import javafx.stage.Stage;

/**
 * Окно отображения хода загрузки или скачивания
 */
public class ProgressStage extends Stage {

    private final ProgressController controller;

    public void setMessage(String message) {
        controller.setMessage(message);
    }

    public void setProgress(double progress) {
        controller.setProgress(progress);
    }

    public void setFileName(String fileName) {
        controller.setFileName(fileName);
    }

    public ProgressStage(ProgressController controller) {
        this.controller = controller;
    }
}
