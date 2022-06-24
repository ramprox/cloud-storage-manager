package client.ui.controllers;

import client.ui.interfaces.SideEventsListener;
import client.ui.model.FileInfoView;
import client.ui.stages.SearchStage;
import interop.dto.fileinfo.FileInfo;
import interop.dto.fileinfo.FileType;
import interop.service.FileInfoService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;

import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class AbstractSideController implements SideEventsListener {

    @FXML protected SideController sideController;

    protected final FileInfoService fileInfoService;

    protected AbstractSideController(FileInfoService fileInfoService) {
        this.fileInfoService = fileInfoService;
    }

    public boolean isFocused() {
        return sideController.isTableFocused();
    }

    protected abstract SearchStage getSearchStage();

    public void createFile(FileType type) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setHeaderText(type == FileType.FILE ? "Введите имя нового файла" : "Введите имя новой папки");
        dialog.showAndWait().ifPresent(fileName -> {
            internalCreateFile(type, fileName);
        });
    }

    protected abstract void internalCreateFile(FileType type, String fileName);

    public void rename() {
        FileInfo selectedFileInfo = sideController.getSelectedItem().getFileInfo();
        if (selectedFileInfo.equals(FileInfo.PARENT_DIR)) {
            return;
        }
        String oldFileName = selectedFileInfo.getFileName();
        TextInputDialog dialog = new TextInputDialog();
        dialog.setHeaderText("Введите новое имя");
        dialog.showAndWait().ifPresent(newFileName -> {
            if (!newFileName.equals("") && !newFileName.equals(oldFileName)) {
                internalRename(oldFileName, newFileName);
            }
        });
    }

    protected abstract void internalRename(String oldFileName, String newFileName);

    public void changeDir(FileInfo fileInfo) {
        Path currentPath = Paths.get(sideController.getCurrentPath());
        Path newPath;
        if (fileInfo.equals(FileInfo.PARENT_DIR)) {
            newPath = currentPath.getParent();
        } else {
            newPath = currentPath.resolve(fileInfo.getFileName());
        }
        internalChangeDir(newPath);
    }

    protected abstract void internalChangeDir(Path newPath);

    public void delete() {
        FileInfoView selectedFileInfoView = sideController.getSelectedItem();
        FileInfo selectedFileInfo = selectedFileInfoView.getFileInfo();
        String fileType = selectedFileInfo.getType() == FileType.DIR ? "директорию " : "файл ";
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Удалить " + fileType +
                "\"" + selectedFileInfo.getFileName() + "\" ?");
        if (alert.showAndWait().get() == ButtonType.OK) {
            internalDelete(selectedFileInfoView);
        }
    }

    protected abstract void internalDelete(FileInfoView selectedFileInfoView);

}
