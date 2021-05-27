package client.MainWindow;

import client.DialogWindows.Connection.ConnectionStage;
import client.MainWindow.table.FileNameCellFactory;
import client.model.FileInfo;
import client.model.FileNameType;
import client.model.FileType;
import client.network.Client;
import javafx.application.Platform;
import javafx.collections.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class MainWindowController {

    private Client client;
    private Path currentClientDir;
    private String currentServerDir;
    @FXML
    private TableView<FileInfo> clientTable;
    @FXML
    private TableColumn<FileInfo, FileNameType> clientFileName;
    @FXML
    private TableColumn<FileInfo, Long> clientFileSize;
    @FXML
    private TableColumn<FileInfo, String> clientFileDate;
    @FXML
    private ComboBox<File> clientDrives;
    @FXML
    private Label clientPath;
    @FXML
    private MenuItem menuItemConnect;
    @FXML
    private MenuItem menuItemDisconnect;
    @FXML
    private TableView<FileInfo> serverTable;
    @FXML
    private TableColumn<FileInfo, FileNameType> serverFileName;
    @FXML
    private TableColumn<FileInfo, Long> serverFileSize;
    @FXML
    private TableColumn<FileInfo, String> serverFileDate;
    @FXML
    private Label serverPath;

    private final String parentDir = "[ . . ]";

    private Set<FileInfo> treeSet = new TreeSet<>();
    private Set<FileInfo> serverTreeSet = new TreeSet<>();

    /**
     * Начальная инициализация
     */
    public void init() {
        clientFileName.setCellValueFactory(new PropertyValueFactory<>("fileNameType"));
        clientFileSize.setCellValueFactory(new PropertyValueFactory<>("size"));
        clientFileDate.setCellValueFactory(new PropertyValueFactory<>("lastModified"));
        clientFileName.setCellFactory(new FileNameCellFactory<>());
        serverFileName.setCellValueFactory(new PropertyValueFactory<>("fileNameType"));
        serverFileSize.setCellValueFactory(new PropertyValueFactory<>("size"));
        serverFileDate.setCellValueFactory(new PropertyValueFactory<>("lastModified"));
        serverFileName.setCellFactory(new FileNameCellFactory<>());
        currentClientDir = Paths.get(System.getProperty("user.home"));
        invalidateClientTable();
        clientTable.setOnMouseClicked(event -> {
            if(event.getClickCount() == 2) {
                FileInfo fileInfo = clientTable.getSelectionModel().getSelectedItem();
                if (fileInfo != null) {
                    if(fileInfo.getFileNameType().getName().equals(parentDir)) {
                        changeDirToParentOnClient();
                    } else if(fileInfo.getFileNameType().getType().equals(FileType.Directory)) {
                        setCurrentDirectoryOnClient(fileInfo.getFileNameType().getName());
                    }
                }
            }
        });
        serverTable.setOnMouseClicked(event -> {
            if(event.getClickCount() == 2) {
                FileInfo fileInfo = serverTable.getSelectionModel().getSelectedItem();
                if(fileInfo != null) {
                    if(fileInfo.getFileNameType().getName().equals(parentDir)) {
                        client.changeServerDirectory("..");
                    } else if(fileInfo.getFileNameType().getType().equals(FileType.Directory)) {
                        client.changeServerDirectory(fileInfo.getFileNameType().getName());
                    }
                }
            }
        });
        File[] paths = File.listRoots();
        clientDrives.setItems(FXCollections.observableArrayList(paths));
        clientDrives.getSelectionModel().select(currentClientDir.getRoot().toFile());
        clientPath.setText(currentClientDir.toString());
    }

    private void changeDirToParentOnClient() {
        Path parentPath = currentClientDir.getParent();
        if(parentPath != null) {
            setCurrentDirectoryOnClient(parentPath.toString());
        }
    }

    private void setCurrentDirectoryOnClient(String path) {
        Path tempPath = Paths.get(path);
        currentClientDir = currentClientDir.resolve(tempPath);
        invalidateClientTable();
        clientPath.setText(currentClientDir.toString());
        treeSet = new TreeSet<>();
    }

    private void invalidateClientTable() {
        treeSet.clear();
        if(currentClientDir.getParent() != null) {
            treeSet.add(new FileInfo(new FileNameType(parentDir, FileType.Directory), null, null));
        }
        File[] filesInCurDir = currentClientDir.toFile().listFiles();
        if(filesInCurDir != null) {
            for (File file : filesInCurDir) {
                FileInfo fileInfo;
                if (Files.isDirectory(file.toPath())) {
                    fileInfo = new FileInfo(new FileNameType(file.getName(), FileType.Directory), null, new Date(file.lastModified()));
                } else {
                    fileInfo = new FileInfo(new FileNameType(file.getName(), FileType.File), file.length(), new Date(file.lastModified()));
                }
                treeSet.add(fileInfo);
            }
        }
        clientTable.getItems().clear();
        clientTable.getItems().addAll(treeSet);
        clientTable.refresh();
    }

    private void invalidateServerTable(FileInfo[] fileInfos) {
        serverTreeSet.clear();
        if(!currentServerDir.equals("~" + File.separator)) {
            serverTreeSet.add(new FileInfo(new FileNameType("[ . . ]", FileType.Directory), null, null));
        }
        serverTreeSet.addAll(Arrays.asList(fileInfos));
        serverTable.getItems().clear();
        serverTable.getItems().addAll(serverTreeSet);
        serverTable.refresh();
    }

    @FXML
    public void exitClickAction(ActionEvent actionEvent) {
        closeConnection();
    }

    public void closeConnection() {
        closeChannel();
        Platform.exit();
    }

    private void closeChannel() {
        if(client != null) {
            client.disconnect();
        }
    }

    @FXML
    public void connectClickAction(ActionEvent actionEvent) {
        client = new Client();
        client.setClientAuthorizedCallback(this::authCallback);
        client.setAuthErrorCallback(this::authErrorCallback);
        client.setReadedFileInfo(this::readedFileInfoFromServer);
        client.setCreatedDirOnServer(this::createdDirOnServer);
        client.setCreatedFileOnServer(this::createdFileOnServer);
        client.setRenamedFileOnServer(this::renamedFileOnServer);
        client.setDeletedFileOnServer(this::deletedFileOnServer);
        client.setChannelActive(this::channelActive);
        client.connect();
    }

    private void channelActive() {
        Platform.runLater(() -> {
            try {
                ConnectionStage connectionStage = new ConnectionStage();
                connectionStage.showAndWait();
                ButtonType btnType = connectionStage.getDialogResult();
                if(btnType == ButtonType.OK) {
                    client.authentication(connectionStage.getLogin(), connectionStage.getPassword());
                } else if(btnType == ButtonType.APPLY) {
                    client.registration(connectionStage.getLogin(), connectionStage.getPassword());
                } else {
                    client.disconnect();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
    }

    private void createdDirOnServer(FileInfo fileInfo) {
        serverTreeSet.add(fileInfo);
        serverTable.getItems().add(fileInfo);
        serverTable.refresh();
    }

    private void createdFileOnServer(FileInfo fileInfo) {
        serverTreeSet.add(fileInfo);
        serverTable.getItems().add(fileInfo);
        serverTable.refresh();
    }

    private FileInfo oldFileInfo;

    private void renamedFileOnServer(FileInfo newFileInfo) {
        serverTreeSet.remove(this.oldFileInfo);
        serverTreeSet.add(newFileInfo);
        serverTable.getItems().remove(this.oldFileInfo);
        serverTable.getItems().add(newFileInfo);
        serverTable.refresh();
    }

    private void deletedFileOnServer() {
        serverTreeSet.remove(this.oldFileInfo);
        serverTable.getItems().remove(this.oldFileInfo);
        serverTable.refresh();
    }

    public void clientDriveChanged(ActionEvent actionEvent) {
        File file = clientDrives.getSelectionModel().getSelectedItem();
        if(file != null) {
            setCurrentDirectoryOnClient(file.toString());
        }
    }

    private void authCallback() {
        menuItemConnect.setDisable(true);
        menuItemDisconnect.setDisable(false);
        System.out.println("Авторизация прошла");
    }

    private void authErrorCallback(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR, message);
            alert.showAndWait();
            channelActive();
        });
    }

    private void readedFileInfoFromServer(FileInfo[] fileInfos, String path) {
        currentServerDir = path;
        Platform.runLater(() -> serverPath.setText(path));
        invalidateServerTable(fileInfos);
    }

    public void disconnectClick(ActionEvent actionEvent) {
        closeChannel();
        menuItemConnect.setDisable(false);
        menuItemDisconnect.setDisable(true);
        serverTreeSet.clear();
        serverTable.getItems().clear();
        serverPath.setText("");
    }

    public void createDirOnClient(ActionEvent actionEvent) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setHeaderText("Создать новую директорию");
        dialog.showAndWait().ifPresent(result -> {
            try {
                if(!result.equals("")) {
                    Path path = Files.createDirectory(currentClientDir.resolve(result));
                    File file = path.toFile();
                    FileInfo fileInfo = new FileInfo(new FileNameType(file.getName(), FileType.Directory),
                            null, new Date(file.lastModified()));
                    treeSet.add(fileInfo);
                    clientTable.getItems().add(fileInfo);
                    clientTable.refresh();
                }
            } catch (IOException ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR, ex.getMessage());
                alert.showAndWait();
            }
        });
    }

    public void createFileOnClient(ActionEvent actionEvent) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setHeaderText("Создать новый файл");
        dialog.showAndWait().ifPresent(result -> {
            try {
                if(!result.equals("")) {
                    Path path = Files.createFile(currentClientDir.resolve(result));
                    File file = path.toFile();
                    FileInfo fileInfo = new FileInfo(new FileNameType(file.getName(), FileType.File),
                            file.length(), new Date(file.lastModified()));
                    treeSet.add(fileInfo);
                    clientTable.getItems().add(fileInfo);
                    clientTable.refresh();
                }
            } catch (IOException ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR, ex.getMessage());
                alert.showAndWait();
            }
        });
    }

    public void createDirOnServer(ActionEvent actionEvent) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setHeaderText("Создать новую директорию");
        dialog.showAndWait().ifPresent(result -> {
            if(!result.equals("")) {
                client.createDir(result);
            }
        });
    }

    public void createFileOnServer(ActionEvent actionEvent) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setHeaderText("Создать новый файл");
        dialog.showAndWait().ifPresent(result -> {
            if(!result.equals("")) {
                client.createFile(result);
            }
        });
    }

    public void renameFileOnClient(ActionEvent actionEvent) {
        final FileInfo fileInfo = clientTable.getSelectionModel().getSelectedItem();
        if(fileInfo != null) {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setHeaderText("Новое имя");
            dialog.showAndWait().ifPresent(result -> {
                if(!result.equals("")) {
                    try {
                        File file = Files.move(currentClientDir.resolve(fileInfo.getFileNameType().getName()),
                                currentClientDir.resolve(result)).toFile();
                        treeSet.remove(fileInfo);
                        if(fileInfo.getFileNameType().getType().equals(FileType.Directory)) {
                            FileNameType fileNameType = new FileNameType(result, FileType.Directory);
                            FileInfo fileInfo1 = new FileInfo(fileNameType, null, new Date(file.lastModified()));
                            treeSet.add(fileInfo1);
                            clientTable.getItems().remove(fileInfo);
                            clientTable.getItems().add(fileInfo1);
                            clientTable.refresh();
                        } else {
                            FileNameType fileNameType = new FileNameType(result, FileType.File);
                            FileInfo fileInfo1 = new FileInfo(fileNameType, file.length(), new Date(file.lastModified()));
                            treeSet.add(fileInfo1);
                            clientTable.getItems().remove(fileInfo);
                            clientTable.getItems().add(fileInfo1);
                            clientTable.refresh();
                        }
                    } catch (IOException e) {
                        Alert alert = new Alert(Alert.AlertType.ERROR, e.getMessage());
                        alert.showAndWait();
                    }
                }
            });
        }
    }

    public void renameFileOnServer(ActionEvent actionEvent) {
        final FileInfo fileInfo = serverTable.getSelectionModel().getSelectedItem();
        if(fileInfo != null) {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setHeaderText("Новое имя");
            dialog.showAndWait().ifPresent(result -> {
                if (!result.equals("") && !result.equals(fileInfo.getFileNameType().getName())) {
                    this.oldFileInfo = fileInfo;
                    client.renameFile(fileInfo.getFileNameType().getName(), result);
                }
            });
        }
    }

    public void deleteFileOnClient(ActionEvent actionEvent) {
        final FileInfo fileInfo = clientTable.getSelectionModel().getSelectedItem();
        if(fileInfo != null) {
            String fileType = fileInfo.getFileNameType().getType() == FileType.Directory ? "директорию " : "файл ";
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Удалить " + fileType +
                    fileInfo.getFileNameType().getName() + "?");
            if(alert.showAndWait().get() == ButtonType.OK) {
                Path filePath = currentClientDir.resolve(fileInfo.getFileNameType().getName());
                try {
                    if(Files.isDirectory(filePath)) {
                        Files.walkFileTree(filePath, new SimpleFileVisitor<Path>() {
                            @Override
                            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                                Files.delete(dir);
                                return FileVisitResult.CONTINUE;
                            }

                            @Override
                            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                                Files.delete(file);
                                return FileVisitResult.CONTINUE;
                            }
                        });
                    } else {
                        Files.delete(filePath);
                    }
                } catch (IOException e) {
                    Alert alertError = new Alert(Alert.AlertType.ERROR, e.getMessage());
                    alertError.showAndWait();
                }
                treeSet.remove(fileInfo);
                clientTable.getItems().remove(fileInfo);
                clientTable.refresh();
            }
        }
    }

    public void deleteFileOnServer(ActionEvent actionEvent) {
        final FileInfo fileInfo = serverTable.getSelectionModel().getSelectedItem();
        if(fileInfo != null) {
            String fileType = fileInfo.getFileNameType().getType() == FileType.Directory ? "директорию " : "файл ";
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Удалить " + fileType +
                    fileInfo.getFileNameType().getName() + "?");
            if(alert.showAndWait().get() == ButtonType.OK) {
                this.oldFileInfo = fileInfo;
                client.deleteFile(fileInfo.getFileNameType().getName());
            }
        }
    }
}
