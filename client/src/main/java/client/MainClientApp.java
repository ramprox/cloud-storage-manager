package client;

import client.controllers.MainWindowController;
import client.stages.ProgressStage;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.stage.Stage;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;

/**
 * Главный класс, содержащий точку входа в приложение
 */
public class MainClientApp extends Application {

    /**
     * Запуск JavaFX приложения
     * @param primaryStage главное окно приложения
     * @throws Exception может возникнуть при загрузке ресурса для главного окна
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/windows/MainWindow.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        MainWindowController controller = loader.getController();
        controller.init();
        primaryStage.setOnCloseRequest(event -> controller.closeConnection());
        primaryStage.setTitle("Cloud storage manager");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Точка входа в приложение
     * @param args аргументы командной строки
     */
    public static void main(String[] args) throws IOException {
        launch(args);
    }
}
