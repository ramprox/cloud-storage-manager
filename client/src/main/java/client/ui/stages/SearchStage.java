package client.ui.stages;

import client.ui.controllers.SearchController;
import client.ui.model.FileInfoView;
import javafx.stage.Stage;

import java.util.Collection;

/**
 * Окно отображающее результаты поиска файла
 */
public class SearchStage extends Stage {

    private final SearchController controller;

    public SearchStage(SearchController controller) {
        this.controller = controller;
    }

    public void addItems(Collection<? extends FileInfoView> files) {
        controller.addItems(files);
    }

}
