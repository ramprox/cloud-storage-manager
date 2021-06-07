package client.utils.tablecellfactories;

import client.interfaces.ClickContextMenuCallback;
import client.model.FileInfoView;
import client.utils.ApplicationUtil;
import interop.model.fileinfo.FileNameType;
import interop.model.fileinfo.FileType;
import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.util.Callback;

/**
 * Класс для создания ячейки, отображающего имя и тип файла
 * @param <S>
 * @param <T>
 */
public class FileNameCellFactory<S extends FileInfoView, T extends FileNameType>
        implements Callback<TableColumn<S, T>, TableCell<S, T>> {

    private ClickContextMenuCallback clickCallback;

    public FileNameCellFactory(ClickContextMenuCallback callback) {
        this.clickCallback = callback;
    }

    @Override
    public TableCell<S, T> call(TableColumn<S, T> param) {
        final Label label = new Label();
        ContextMenu contextMenu = new ContextMenu();
        TableCell<S, T> cell = new TableCell<S, T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                if(item == getItem()) {
                    return;
                }
                super.updateItem(item, empty);
                if(item != null) {
                    String filename = item.getFileName();
                    label.setText(filename);
                    ImageView imageViewDir = null;
                    if(item.getFileType().equals(FileType.DIR)) {
                        if(filename.equals("[ . . ]")) {
                            imageViewDir = new ImageView(ApplicationUtil.IMG_PARENT_DIR);
                        } else {
                            imageViewDir = new ImageView(ApplicationUtil.IMG_DIRECTORY);
                        }
                    } else if(item.getFileType().equals(FileType.FILE)) {
                        imageViewDir = new ImageView(ApplicationUtil.IMG_FILE);
                    }
                    label.setGraphic(imageViewDir);
                    label.setAlignment(Pos.CENTER_LEFT);
                    label.prefWidthProperty().bind(Bindings.divide(widthProperty(), 1));
                    super.setGraphic(label);
                }
            }
        };
        MenuItem menuItem = new MenuItem();
        menuItem.setText("Размер");
        menuItem.setOnAction(event -> {
            if (clickCallback != null) {
                clickCallback.call();
            }});
        contextMenu.getItems().add(menuItem);
        cell.setContextMenu(contextMenu);
        return cell;
    }
}
