package client.MainWindow.table;

import client.MainClientApp;
import client.MainWindow.fileinfos.FileInfo;
import client.MainWindow.fileinfos.FileNameType;
import client.MainWindow.fileinfos.FileType;
import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Callback;

public class FileNameCellFactory<S extends FileInfo, T extends FileNameType>
        implements Callback<TableColumn<S, T>, TableCell<S, T>> {

    private final Image imgDirectory = new Image(MainClientApp.class.getResource("/images/dir.png").toString());
    private final Image imgFile = new Image(MainClientApp.class.getResource("/images/file.png").toString());
    private final Image imgParentDir = new Image(MainClientApp.class.getResource("/images/parentdir.png").toString());

    @Override
    public TableCell<S, T> call(TableColumn<S, T> param) {
        final Label label = new Label();
        TableCell<S, T> cell = new TableCell<S, T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                if(item == getItem()) {
                    return;
                }
                super.updateItem(item, empty);
                if(item != null) {
                    String filename = item.getName();
                    label.setText(filename);
                    ImageView imageViewDir = null;
                    if(item.getType() == FileType.Directory) {
                        if(filename.equals("[ . . ]")) {
                            imageViewDir = new ImageView(imgParentDir);
                        } else {
                            imageViewDir = new ImageView(imgDirectory);
                        }
                    } else if(item.getType() == FileType.File) {
                        imageViewDir = new ImageView(imgFile);
                    }
                    label.setGraphic(imageViewDir);
                    label.setAlignment(Pos.CENTER_LEFT);
                    label.prefWidthProperty().bind(Bindings.divide(widthProperty(), 1));
                    super.setGraphic(label);
                }
            }
        };
        return cell;
    }
}
