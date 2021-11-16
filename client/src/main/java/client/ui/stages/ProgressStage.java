package client.ui.stages;

import client.ui.MainClientApp;
import client.ui.controllers.ProgressController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Окно отображения хода загрузки или скачивания
 */
public class ProgressStage extends Stage {

    private ProgressController controller;

    public void setMessage(String message) {
        controller.setMessage(message);
    }

    public void setProgress(double progress) {
        controller.setProgress(progress);
    }

    public void setFileName(String fileName) {
        controller.setFileName(fileName);
    }

    public ProgressStage(String title) {
        try {
            setTitle(title);
            FXMLLoader loader = new FXMLLoader(MainClientApp.class.getResource("/windows/Progress.fxml"));
            Parent root = loader.load();
            controller = loader.getController();
            Scene scene = new Scene(root);
            setScene(scene);
            setResizable(false);
            sizeToScene();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
