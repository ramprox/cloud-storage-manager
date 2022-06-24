package client.ui.controllers;

import client.config.ImageLocation;
import client.ui.model.FileInfoView;
import client.ui.service.FileInfoViewComparator;
import interop.dto.fileinfo.FileType;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * Класс окна, в котором отображаются найденные файлы
 */
@Component
@Scope("prototype")
public class SearchController {

    @FXML
    private ListView<FileInfoView> foundedFiles;

    private final FileInfoViewComparator comparators;

    private final ImageLocation imageLocation;

    @Autowired
    public SearchController(FileInfoViewComparator comparators, ImageLocation imageLocation) {
        this.comparators = comparators;
        this.imageLocation = imageLocation;
    }

    /**
     * Добавляет коллекцию экземпляров класса FileInfoView
     *
     * @param list добавляемая коллекция
     */
    public void addItems(Collection<? extends FileInfoView> list) {
        foundedFiles.getItems().addAll(list);
        FXCollections.sort(foundedFiles.getItems(),
                comparators.byName(TableColumn.SortType.ASCENDING));
        foundedFiles.refresh();
    }

    @FXML
    public void initialize() {
        foundedFiles.setCellFactory(param -> new FoundedFileCell());
    }

    /**
     * Класс, определяющий внешний вид ячейки ListView
     */
    private class FoundedFileCell extends ListCell<FileInfoView> {

        @Override
        protected void updateItem(FileInfoView item, boolean empty) {
            if (item != null) {
                Label label = new Label();
                ImageView imageViewDir = null;
                if (item.getFileInfo().getType() == FileType.DIR) {
                    imageViewDir = new ImageView(new Image(SearchController.this.imageLocation.getImageDir()));
                } else if (item.getFileInfo().getType() == FileType.FILE) {
                    imageViewDir = new ImageView(new Image(SearchController.this.imageLocation.getImageFile()));
                }
                label.setGraphic(imageViewDir);
                label.setAlignment(Pos.CENTER_LEFT);
                label.setText(item.getFileInfo().getFileName());
                setGraphic(label);
            }
        }
    }
}
