package client.controllers;

import client.MainClientApp;
import client.model.FileInfoView;
import client.utils.ApplicationUtil;
import client.utils.Comparators;
import interop.model.fileinfo.FileType;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.*;

import java.util.Collection;

/**
 * Класс окна, в котором отображаются найденные файлы
 */
public class SearchController {
    @FXML
    private ListView<FileInfoView> foundedFiles;

    /**
     * Добавляет коллекцию экземпляров класса FileInfoView
     * @param list добавляемая коллекция
     */
    public void addItems(Collection<? extends FileInfoView> list) {
        foundedFiles.getItems().addAll(list);
        FXCollections.sort(foundedFiles.getItems(), Comparators.comparatorByName(TableColumn.SortType.ASCENDING));
        foundedFiles.refresh();
    }

    /**
     * Начальная инициализация. Происходит назначение внешнего вида ячейки ListView,
     * в котором отображаются найденные файлы
     */
    public void init() {
        foundedFiles.setCellFactory(param -> new FoundedFileCell());
    }

    /**
     * Класс, определяющий внешний вид ячейки ListView
     */
    class FoundedFileCell extends ListCell<FileInfoView> {
        @Override
        protected void updateItem(FileInfoView item, boolean empty) {
            if(item != null) {
                Label label = new Label();
                ImageView imageViewDir = null;
                if(item.getType().equals(FileType.DIR)) {
                    imageViewDir = new ImageView(ApplicationUtil.IMG_DIRECTORY);
                } else if(item.getType().equals(FileType.FILE)) {
                    imageViewDir = new ImageView(ApplicationUtil.IMG_FILE);
                }
                label.setGraphic(imageViewDir);
                label.setAlignment(Pos.CENTER_LEFT);
                label.setText(item.getName());
                setGraphic(label);
            }
        }
    }
}
