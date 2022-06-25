package client.ui;

import client.config.FxmlLocation;
import client.config.MainConfig;
import client.ui.controllers.MainWindowController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.IOException;
import java.util.Locale;

/**
 * Главный клиентский класс
 */
public class MainClientApp extends Application {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private FxmlLocation fxmlLocation;

    @Autowired
    private MessageSource messageSource;

    @Override
    public void init() {
        this.context = new AnnotationConfigApplicationContext(MainConfig.class);
        this.context.getAutowireCapableBeanFactory().autowireBean(this);

    }

    /**
     * Запуск JavaFX приложения
     *
     * @param primaryStage главное окно приложения
     */
    @Override
    public void start(Stage primaryStage) {
        AnnotationConfigApplicationContext annotationConfigApplicationContext =
                (AnnotationConfigApplicationContext) context;
        annotationConfigApplicationContext.getBeanFactory()
                .registerSingleton("mainStage", primaryStage);
        configurePrimaryStage(primaryStage);
        primaryStage.show();
    }

    public void configurePrimaryStage(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(MainClientApp.class.getResource(fxmlLocation.getMainWindow()));
            loader.setControllerFactory(context::getBean);
            Parent root = loader.load();
            Scene scene = new Scene(root);
            MainWindowController controller = loader.getController();
            scene.getAccelerators().put(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN), controller::createFileClick);
            scene.getAccelerators().put(new KeyCodeCombination(KeyCode.D, KeyCombination.CONTROL_DOWN), controller::createDirClick);
            scene.getAccelerators().put(new KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_DOWN), controller::renameFile);
            scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F5), controller::copyClick);
            scene.getAccelerators().put(new KeyCodeCombination(KeyCode.DELETE), controller::deleteFile);
            stage.setOnCloseRequest(event -> controller.close());
            stage.setTitle(messageSource.getMessage("mainWindow.title", null, Locale.getDefault()));
            stage.setScene(scene);
        } catch (IOException ex) {
            System.out.println("Не могу сконфигурировать главное окно. " + ex.getMessage());
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void stop() {
        AnnotationConfigApplicationContext context = (AnnotationConfigApplicationContext)this.context;
        context.close();
    }
}
