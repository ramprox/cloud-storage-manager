package client.ui.controllers;

import client.events.ConnectionState;
import client.ui.model.FileInfoView;
import interop.dto.fileinfo.FileType;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Контроллер главного окна
 */
@Component
public class MainWindowController {

    @FXML private ClientSideController clientSideController;

    @FXML private ServerSideController serverSideController;

    private final BooleanProperty sign = new SimpleBooleanProperty(false);

    public final boolean getSign() {
        return sign.get();
    }

    public final void setSign(boolean sign) {
        this.sign.set(sign);
    }

    public BooleanProperty signProperty() {
        return sign;
    }

    @EventListener
    public void connectionListener(ConnectionState stateChanged) {
        Platform.runLater(() -> sign.set(stateChanged.isState()));
    }

    // ------------------------- Обработка нажатия на кнопках меню ---------------------------

    /**
     * Соединение с сервером (при щелчке на кнопке "Menu" -> "Соединение" -> "Соединиться...")
     */
    public void connectClick() {
        serverSideController.connect();
    }

    /**
     * Происходит при щелчке левой кнопкой мыши по кнопке "Создать файл" в нижней части главного окна
     */
    public void createFileClick() {
        createFile(FileType.FILE);
    }

    /**
     * Происходит при щелчке левой кнопкой мыши по кнопке "Создать папку" в нижней части главного окна
     */
    public void createDirClick() {
        createFile(FileType.DIR);
    }

    /**
     * Общая логика обработки нажатия кнопки "Создать файл" или "Создать папку" для стороны клиента и сервера
     *
     * @param fileType тип создаваемого файла
     */
    private void createFile(FileType fileType) {
        if (clientSideController.isFocused()) {
            clientSideController.createFile(fileType);
        } else if (serverSideController.isFocused() && getSign()) {
            serverSideController.createFile(fileType);
        } else {
            showError("Не выбрано место создания! Выберите место создания файла кликнув на одной из таблиц");
        }
    }

    /**
     * Общая для клиента и сервера логика обработки события кнопки "Переименовать"
     */
    public void renameFile() {
        if (clientSideController.isFocused()) {
            clientSideController.rename();
        } else if (serverSideController.isFocused() && getSign()) {
            serverSideController.rename();
        } else {
            showError("Не выбран ни один файл или выбраны сразу в двух таблицах");
        }
    }

    /**
     * Общая для клиента и сервера логика обработки события нажатия кнопки "Удалить"
     */
    public void deleteFile() {
        if (clientSideController.isFocused()) {
            clientSideController.delete();
        } else if (serverSideController.isFocused() && getSign()) {
            serverSideController.delete();
        } else {
            showError("Не выбран ни один файл или выбраны сразу в двух таблицах");
        }
    }

    /**
     * Общая для клиента и сервера логика обработки события кнопки "Копировать"
     */
    public void copyClick() {
        if (clientSideController.isFocused()) {
            List<FileInfoView> selectedFileInfoViews = clientSideController.getSelectedItems();
            if (selectedFileInfoViews.size() > 0) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Скопировать файлы в хранилище?");
                if (alert.showAndWait().get() == ButtonType.OK) {
                    String source = clientSideController.getCurrentPath();
                    List<String> selectedItems = clientSideController.getSelectedItems()
                            .stream()
                            .map(fileInfoView -> Paths.get(fileInfoView.getFileInfo().getFileName()).toString())
                            .collect(Collectors.toList());
                    serverSideController.upload(source, selectedItems);
                }
            }
        } else if (serverSideController.isFocused() && getSign()) {
            List<FileInfoView> selectedFileInfoViews = serverSideController.getSelectedItems();
            if (selectedFileInfoViews.size() > 0) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Скопировать файлы из хранилища?");
                if (alert.showAndWait().get() == ButtonType.OK) {
                    String destination = clientSideController.getCurrentPath();
                    serverSideController.download(destination);
                }
            }
        } else {
            showError("Не выбран ни один файл или выбраны сразу в двух таблицах");
        }
    }

    /**
     * Закрытие соединения и выход из программы
     */
    public void close() {
        if (getSign()) {
            serverSideController.close();
        }
    }

    /**
     * Происходит после нажатия на кнопке "Отсоединиться от сервера"
     */
    public void disconnectClick() {
        serverSideController.disconnectClick();
    }

//    -----------------------------------------------------------------------

    /**
     * Отображение окна с сообщением об ошибке
     *
     * @param message сообщение об ошибке
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.showAndWait();
    }
}
