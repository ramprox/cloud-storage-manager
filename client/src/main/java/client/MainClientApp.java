package client;

import client.controllers.MainWindowController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.stage.Stage;

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
        scene.focusOwnerProperty().addListener((prop, oldNode, newNode) -> {controller.focusOwnerListener(oldNode, newNode);});
        primaryStage.show();
    }

    /**
     * Точка входа в приложение
     * @param args аргументы командной строки
     */
    public static void main(String[] args) {
        launch(args);
    }
}
