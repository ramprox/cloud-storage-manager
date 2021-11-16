package client.ui.stages;

import client.ui.MainClientApp;
import client.ui.controllers.SearchController;
import client.ui.model.FileInfoView;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Collection;

/**
 * Окно отображающее результаты поиска файла
 */
public class SearchStage extends Stage {

    private SearchController controller;

    public void addItems(Collection<? extends FileInfoView> files) {
        controller.addItems(files);
    }

    public SearchStage() {
        try {
            setTitle("Поиск");
            FXMLLoader loader = new FXMLLoader(MainClientApp.class.getResource("/windows/SearchFiles.fxml"));
            Parent root = loader.load();
            controller = loader.getController();
            controller.init();
            Scene scene = new Scene(root);
            setScene(scene);
            setResizable(false);
            sizeToScene();
        } catch (IOException ex) {
            System.out.println("Внутренняя ошибка");
        }
    }
}
