package client.ui.controllers;

import client.network.Client;
import client.ui.interfaces.ServerEventsListener;
import client.ui.interfaces.SideEventsListener;
import client.ui.model.FileInfoView;
import client.ui.stages.AuthStage;
import client.ui.stages.ProgressStage;
import client.ui.stages.SearchStage;
import interop.Command;
import interop.dto.AuthDto;
import interop.dto.fileinfo.FileInfo;
import interop.dto.fileinfo.FileType;
import interop.service.FileInfoService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.net.ConnectException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class ServerSideController extends AbstractSideController
        implements SideEventsListener, ServerEventsListener {

    private Client client;

    private ProgressStage progressStage;

    private AuthStage authStage;

    @Value("${serverUserRootSymbol}")
    private String userRootSymbol;

    protected ServerSideController(FileInfoService fileInfoService) {
        super(fileInfoService);
    }

    public List<FileInfoView> getSelectedItems() {
        return sideController.getSelectedItems();
    }

    @FXML
    private void initialize() {
        sideController.setDrivesVisible(false);
        sideController.setSideEventProcessable(this);
    }

    protected abstract AuthStage getAuthStage();

    protected abstract ProgressStage getProgressStage();

    protected abstract Client getClient();

    public void connect() {
        Client client = getClient();
        this.client = client;
        client.connect();
    }

    @Override
    protected void internalCreateFile(FileType type, String fileName) {
        String newFilePath = Paths.get(sideController.getCurrentPath(), fileName).toString();
        if (type == FileType.FILE) {
            client.sendCommand(Command.CREATE_FILE, newFilePath);
        } else {
            client.sendCommand(Command.CREATE_DIR, newFilePath);
        }
    }

    @Override
    protected void internalRename(String oldFileName, String newFileName) {
        String oldFilePath = Paths.get(sideController.getCurrentPath(), oldFileName).toString();
        String newFilePath = Paths.get(sideController.getCurrentPath(), newFileName).toString();
        client.sendCommand(Command.RENAME, oldFilePath, newFilePath);
    }

    @Override
    protected void internalChangeDir(Path newPath) {
        client.sendCommand(Command.CHANGE_DIR, newPath.toString());
    }

    @Override
    protected void internalDelete(FileInfoView selectedFileInfoView) {
        FileInfo selectedFileInfo = selectedFileInfoView.getFileInfo();
        String path = Paths.get(sideController.getCurrentPath(), selectedFileInfo.getFileName()).toString();
        client.sendCommand(Command.DELETE, path);
    }

    @Override
    public void driveChanged(String newPath) {
    }

    @Override
    public void sizeClicked(Path path) {
        client.sendCommand(Command.GET_DIR_SIZE, path.toString());
    }

    @Override
    public void searchFile(String fileName) {
        client.sendCommand(Command.SEARCH, sideController.getCurrentPath(), fileName);
    }

    public void upload(String source, List<String> paths) {
        String destination = sideController.getCurrentPath();
        client.sendCommand(Command.UPLOAD, source, paths, destination);
        startProgress();
    }

    public void download(String destination) {
        List<String> selectedItems = sideController.getSelectedItems()
                .stream()
                .map(fileInfoView -> Paths.get(fileInfoView.getFileInfo().getFileName()).toString())
                .collect(Collectors.toList());
        String currentPath = sideController.getCurrentPath();
        client.sendCommand(Command.DOWNLOAD, currentPath, selectedItems, destination);
        startProgress();
    }

    /**
     * Начальное отображение окна с ходом загрузки или скачивания
     */
    private void startProgress() {
        progressStage = getProgressStage();
        progressStage.setMessage("Ожидание готовности сервера...");
        progressStage.show();
    }

    // ------------------ Обработка ответов от сервера ----------------

    @Override
    public void clientSigned(String currentDir, List<FileInfo> files) {
        currentDirChanged(currentDir, files);
    }

    @Override
    public void channelActivated() {
        Platform.runLater(() -> {
            authStage = getAuthStage();
            authStage.showAndWait();
            ButtonType btnType = authStage.getDialogResult();
            if(btnType == ButtonType.OK || btnType == ButtonType.APPLY) {
                String login = authStage.getLogin();
                String password = authStage.getPassword();
                AuthDto authDtoRequest = new AuthDto(login, password);
                Command command = btnType == ButtonType.OK ? Command.SIGN_IN : Command.SIGN_UP;
                client.sendCommand(command, authDtoRequest);
            } else {
                client.disconnect();
            }
        });
    }

    @Override
    public void currentDirChanged(String newDirName, List<FileInfo> files) {
        Platform.runLater(() -> {
            String curDir = newDirName;
            if (newDirName.equals(userRootSymbol)) {
                curDir = curDir + File.separator;
            }
            sideController.setCurrentPath(curDir);
            sideController.invalidateTable(getFromFileInfo(newDirName, files));
        });
    }

    /**
     * Преобразование из List<FileInfo>, полученного от сервера в List<FileInfoView>
     * для представления в таблице
     *
     * @param newDirPath   новая текущая директория на сервере
     * @param listFileInfo список информации о файлах на сервере
     * @return список преобразованных FileInfo
     */
    private List<FileInfoView> getFromFileInfo(String newDirPath, List<FileInfo> listFileInfo) {
        List<FileInfoView> result = new LinkedList<>();
        if (!newDirPath.equals(userRootSymbol)) {
            result.add(new FileInfoView(FileInfo.PARENT_DIR));
        }
        for (FileInfo fileInfo : listFileInfo) {
            result.add(new FileInfoView(fileInfo));
        }
        return result;
    }

    @Override
    public void fileCreated(FileInfo fileInfo) {
        Platform.runLater(() -> {
            Path path = Paths.get(fileInfo.getFileName());
            Path normalizeCurPath = Paths.get(sideController.getCurrentPath()).normalize();
            if (normalizeCurPath.equals(path.getParent())) {
                String fileName = path.getFileName().toString();
                fileInfo.setFileName(fileName);
                FileInfoView fileInfoView = new FileInfoView(fileInfo);
                sideController.add(fileInfoView);
            }
        });
    }

    @Override
    public void fileRenamed(String oldFilePath, String newFilePath) {
        Platform.runLater(() -> {
            String oldFileName = Paths.get(oldFilePath).getFileName().toString();
            FileInfoView fileInfoView = sideController.getByFileName(oldFileName);
            fileInfoView.getFileInfo().setFileName(new File(newFilePath).getName());
            sideController.sortAndRefreshTable();
        });
    }

    @Override
    public void fileDeleted(String deletedFilePath) {
        Platform.runLater(() -> {
            String oldFileName = Paths.get(deletedFilePath).getFileName().toString();
            FileInfoView fileInfoView = sideController.getByFileName(oldFileName);
            sideController.remove(fileInfoView);
        });
    }

    @Override
    public void progressUpload(double percent, String fileName) {
        Platform.runLater(() -> {
            progressStage.setProgress(percent);
            progressStage.setFileName(fileName);
            progressStage.setMessage("Копирование файлов...");
        });
    }

    @Override
    public void uploadDone(String destination) {
        Platform.runLater(() -> {
            progressStage.close();
            Path path = Paths.get(destination);
            Path currentPath = Paths.get(sideController.getCurrentPath());
            if (!path.relativize(currentPath).startsWith("..")) {
                client.sendCommand(Command.CHANGE_DIR, currentPath.toString());
            }
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                    "Копирование файлов завершено");
            alert.showAndWait();
        });
    }

    @Override
    public void progressDownload(double percent, String fileName) {
        Platform.runLater(() -> {
            progressStage.setProgress(percent);
            progressStage.setFileName(fileName);
            progressStage.setMessage("Копирование файлов...");
        });
    }

    @Override
    public void downloadDone(String downloadingPath) {
        Platform.runLater(() -> {
            progressStage.close();
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                    "Копирование файлов завершено");
            alert.showAndWait();
        });
    }

    @Override
    public void foundedFilesReceived(List<FileInfo> foundedFiles) {
        Platform.runLater(() -> {
            if(foundedFiles.size() > 0) {
                SearchStage search = getSearchStage();
                List<FileInfoView> fileInfoViewList = foundedFiles.stream()
                        .map(fileInfo -> new FileInfoView(fileInfo))
                        .collect(Collectors.toList());
                search.addItems(fileInfoViewList);
                search.show();
            } else {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Ничего не найдено");
                alert.showAndWait();
            }
        });
    }

    @Override
    public void viewDirSizeOnServer(FileInfo fileInfo) {
        Platform.runLater(() -> {
            FileInfoView fileInfoView = sideController.getByFileName(fileInfo.getFileName());
            fileInfoView.getFileInfo().setSize(fileInfo.getSize());
            sideController.refresh();
        });
    }

    @Override
    public void errorReceived(String errorMessage) {
        Platform.runLater(() -> {
            showError(errorMessage);
            if(errorMessage.toLowerCase().contains("логин") ||
                    errorMessage.toLowerCase().contains("уже существует")) {
                channelActivated();
            }
        });
    }

    @Override
    public void exceptionCaught(Throwable cause) {
        Platform.runLater(() -> {
            if(cause instanceof ConnectException) {
                showError("Соединение с сервером отсутствует");
            } else {
                if(authStage.isShowing()) {
                    authStage.close();
                } else if(progressStage.isShowing()) {
                    authStage.close();
                }
                showInfo("Соединение с сервером потеряно");
            }
            setToInitialState();
        });
    }

    /**
     * Отображение окна с сообщением об ошибке
     * @param message сообщение об ошибке
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.showAndWait();
    }

    /**
     * Приведение в исходное состояние
     */
    private void setToInitialState() {
        sideController.clear();
        sideController.setCurrentPath("");
    }

    public void close() {
        if(client != null) {
            client.disconnect();
        }
    }

    /**
     * Происходит после нажатия на кнопке "Отсоединиться от сервера"
     */
    public void disconnectClick() {
        client.disconnect();
        setToInitialState();
        Platform.runLater(() -> showInfo("Соединение закрыто"));
    }
}
