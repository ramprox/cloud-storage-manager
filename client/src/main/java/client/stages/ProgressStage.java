package client.stages;

import client.MainClientApp;
import client.controllers.ProgressController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

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

    public ProgressStage(String title) throws IOException {
        setTitle(title);
        FXMLLoader loader = new FXMLLoader(MainClientApp.class.getResource("/windows/Progress.fxml"));
        Parent root = loader.load();
        controller = loader.getController();
        Scene scene = new Scene(root);
        setScene(scene);
        setResizable(false);
        sizeToScene();
        initModality(Modality.APPLICATION_MODAL);
    }
}
