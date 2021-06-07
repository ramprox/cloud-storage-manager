package client.utils.tablecellfactories;

import client.model.FileInfoView;
import interop.model.fileinfo.FileNameType;
import interop.model.fileinfo.FileType;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;
import java.time.format.DateTimeFormatter;

/**
 * Класс для создания ячейки, отображающего дату и время создания или изменения файла
 * @param <S>
 * @param <T>
 */
public class FileCreateCellFactory<S extends FileInfoView, T extends SimpleStringProperty>
        implements Callback<TableColumn<S, T>, TableCell<S, T>> {

    @Override
    public TableCell<S, T> call(TableColumn<S, T> param) {
        final Label label = new Label();
        TableCell<S, T> cell = new TableCell<S, T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                if(item != null) {
                    if (item == getItem()) {
                        return;
                    }
                    super.updateItem(item, empty);
                    if (item.getValue() != null) {
                        label.setText(item.getValue());
                    } else {
                        label.setText("");
                    }
                    label.setAlignment(Pos.CENTER);
                    label.prefWidthProperty().bind(Bindings.divide(widthProperty(), 1));
                    super.setGraphic(label);
                }
            }
        };
        return cell;
    }
}
