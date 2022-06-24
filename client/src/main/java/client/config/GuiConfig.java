package client.config;

import client.network.Client;
import client.ui.controllers.*;
import client.ui.interfaces.ServerEventsListener;
import client.ui.stages.AuthStage;
import client.ui.stages.ProgressStage;
import client.ui.stages.SearchStage;
import interop.service.FileInfoService;
import interop.service.FileInfoServiceImpl;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.io.IOException;
import java.util.Locale;

@Configuration
@Import(NetworkConfig.class)
public class GuiConfig {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private NetworkConfig networkConfig;

    @Autowired
    private FxmlLocation fxmlLocation;

    private FXMLLoader fxmlLoader(String location) {
        return new FXMLLoader(GuiConfig.class.getResource(location));
    }

    @Bean
    public FileInfoService fileInfoService() {
        return new FileInfoServiceImpl();
    }

    public SearchStage searchStage() {
        String title = context.getMessage("searchStage.title", null, Locale.getDefault());
        try {
            FXMLLoader loader = fxmlLoader(fxmlLocation.getSearchStage());
            loader.setControllerFactory(context::getBean);
            Parent root = loader.load();
            Scene scene = new Scene(root);
            SearchController controller = loader.getController();
            SearchStage stage = new SearchStage(controller);
            stage.setScene(scene);
            stage.setResizable(false);
            stage.sizeToScene();
            stage.setTitle(title);
            return stage;
        } catch (IOException ex) {
            System.out.println("Не могу сконфигурировать окно поиска. " + ex.getMessage());
            throw new RuntimeException(ex);
        }
    }

    public AuthStage authStage() {
        String title = context.getMessage("authStage.title", null, Locale.getDefault());
        try {
            FXMLLoader loader = fxmlLoader(fxmlLocation.getAuthStage());
            Parent root = loader.load();
            AuthController controller = loader.getController();
            AuthStage authStage = new AuthStage(controller);
            configureModalWindow(authStage, root, title);
            authStage.initOwner(context.getBean("mainStage", Stage.class));
            return authStage;
        } catch (IOException ex) {
            System.out.println("Не могу создать окно аутентификации");
            throw new RuntimeException(ex);
        }
    }

    public ProgressStage progressStage() {
        String title = context.getMessage("progressStage.title", null, Locale.getDefault());
        try {
            FXMLLoader loader = fxmlLoader(fxmlLocation.getProgressStage());
            Parent root = loader.load();
            ProgressController controller = loader.getController();
            ProgressStage progressStage = new ProgressStage(controller);
            configureModalWindow(progressStage, root, title);
            return progressStage;
        } catch (IOException ex) {
            System.out.println("Не могу создать окно отображения хода копирования");
            throw new RuntimeException(ex);
        }
    }

    private void configureModalWindow(Stage stage, Parent root, String title) {
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.sizeToScene();
        stage.initStyle(StageStyle.UTILITY);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(title);
    }

    @Bean
    public ClientSideController clientSideController(FileInfoService fileInfoService) {
        return new ClientSideController(fileInfoService) {
            @Override
            public SearchStage getSearchStage() {
                return searchStage();
            }
        };
    }

    @Bean
    public ServerSideController serverSideController(FileInfoService fileInfoService) {
        return new ServerSideController(fileInfoService) {
            @Override
            protected AuthStage getAuthStage() {
                return authStage();
            }

            @Override
            protected SearchStage getSearchStage() {
                return searchStage();
            }

            @Override
            protected ProgressStage getProgressStage() {
                return progressStage();
            }

            @Override
            protected Client getClient() {
                ServerEventsListener serverEventsListener = serverSideController(fileInfoService);
                return networkConfig.client(serverEventsListener);
            }
        };
    }
}
