package client.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FxmlLocation {

    @Value("${mainWindow.fxml}")
    private String mainWindow;

    @Value("${authStage.fxml}")
    private String authStage;

    @Value("${progressStage.fxml}")
    private String progressStage;

    @Value("${searchStage.fxml}")
    private String searchStage;

    public String getMainWindow() {
        return mainWindow;
    }

    public String getAuthStage() {
        return authStage;
    }

    public String getProgressStage() {
        return progressStage;
    }

    public String getSearchStage() {
        return searchStage;
    }
}
