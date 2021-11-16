package client.ui.controllers;

import client.ui.model.FileInfoView;
import client.utils.ApplicationUtil;
import client.ui.service.FileInfoViewComparator;
import interop.model.fileinfo.FileType;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.image.ImageView;

import java.util.Collection;

/**
 * Класс окна, в котором отображаются найденные файлы
 */
public class SearchController {
    @FXML
    private ListView<FileInfoView> foundedFiles;

    /**
     * Добавляет коллекцию экземпляров класса FileInfoView
     *
     * @param list добавляемая коллекция
     */
    public void addItems(Collection<? extends FileInfoView> list) {
        foundedFiles.getItems().addAll(list);
        FXCollections.sort(foundedFiles.getItems(),
                FileInfoViewComparator.byName(TableColumn.SortType.ASCENDING));
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
    private static class FoundedFileCell extends ListCell<FileInfoView> {
        @Override
        protected void updateItem(FileInfoView item, boolean empty) {
            if (item != null) {
                Label label = new Label();
                ImageView imageViewDir = null;
                if (item.getFileInfo().getType() == FileType.DIR) {
                    imageViewDir = new ImageView(ApplicationUtil.IMG_DIRECTORY);
                } else if (item.getFileInfo().getType() == FileType.FILE) {
                    imageViewDir = new ImageView(ApplicationUtil.IMG_FILE);
                }
                label.setGraphic(imageViewDir);
                label.setAlignment(Pos.CENTER_LEFT);
                label.setText(item.getFileInfo().getFileName());
                setGraphic(label);
            }
        }
    }
}
