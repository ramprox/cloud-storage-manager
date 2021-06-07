package client.utils.tablecellfactories;

import client.model.FileInfoView;
import io.netty.util.internal.StringUtil;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleLongProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

import java.text.DecimalFormat;

/**
 * Класс для создания ячейки, отображающего размер файла
 * @param <S>
 * @param <T>
 */
public class FileSizeCellFactory<S extends FileInfoView, T extends SimpleLongProperty>
        implements Callback<TableColumn<S, T>, TableCell<S, T>> {

    private DecimalFormat formatter = new DecimalFormat("###,###,###,###,###,###,###,###");

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
                    Long value = item.getValue();
                    if (value != -1) {
                        String strValue = formatter.format(item.getValue());
                        label.setText(strValue);
                    } else {
                        label.setText("");
                    }
                    label.setAlignment(Pos.CENTER_RIGHT);
                    label.prefWidthProperty().bind(Bindings.divide(widthProperty(), 1));
                    super.setGraphic(label);
                }
            }
        };
        return cell;
    }
}
