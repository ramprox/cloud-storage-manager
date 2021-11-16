package client.ui;

import client.ui.controllers.MainWindowController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Главный клиентский класс
 */
public class MainClientApp extends Application {

    /**
     * Запуск JavaFX приложения
     *
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
}
