package client.controllers;

import client.interfaces.SideEventsProcessable;
import client.stages.*;
import client.interfaces.Presentable;
import client.model.*;
import client.network.Client;
import client.utils.ApplicationUtil;
import interop.model.fileinfo.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.*;

import java.io.*;
import java.net.ConnectException;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Главное окно приложения
 */
public class MainWindowController implements Presentable, SideEventsProcessable {
    @FXML private MenuItem menuItemConnect;
    @FXML private MenuItem menuItemDisconnect;
    @FXML private SideController clientSideController;
    @FXML private SideController serverSideController;
    @FXML private Button newFile;
    @FXML private Button newDir;
    @FXML private Button rename;
    @FXML private Button copy;
    @FXML private Button delete;

    private Client client;                               // объект, через который происходит подключение к серверу
    private boolean isSign;
    private ProgressStage progressStage;                 // окно с отображением хода загрузки и выгрузки файлов
    private ConnectionStage connectionStage;             // диалоговое окно аутентификации

    /**
     * Происходит установка свойств таблиц и колонок клиента и сервера.
     * По умолчанию текущий путь для клиента устанавливается его домашней директорией
     * По этой домашней директории получается список файлов и заносится в таблицу клиента
     */
    public void init() {
        serverSideController.setDrivesVisible(false);
        clientSideController.setSideEventProcessable(this);
        serverSideController.setSideEventProcessable(this);
        File[] rootDirectories = File.listRoots();
        clientSideController.setDrives(rootDirectories);
        Path currentClientPath = Paths.get(ApplicationUtil.START_DIR_FOR_CLIENT);
        clientSideController.selectDrive(currentClientPath.getRoot().toString());
        clientSideController.setCurrentPath(currentClientPath.toString());
        clientSideController.invalidateTable(getListFileInfoViewForClient(currentClientPath));
        addShortcuts();
    }

    /**
     * Добавление горячих клавиш для пунктов меню в нижней части окна
     */
    private void addShortcuts() {
        newFile.getScene().getAccelerators().put(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN), () -> newFile.fire());
        newDir.getScene().getAccelerators().put(new KeyCodeCombination(KeyCode.D, KeyCombination.CONTROL_DOWN), () -> newDir.fire());
        rename.getScene().getAccelerators().put(new KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_DOWN), () -> rename.fire());
        copy.getScene().getAccelerators().put(new KeyCodeCombination(KeyCode.F5), () -> copy.fire());
        delete.getScene().getAccelerators().put(new KeyCodeCombination(KeyCode.DELETE), () -> delete.fire());
    }

    /**
     * Обработка изменения корневой директории на стороне клиента
     * @param newPath путь новой корневой директории
     */
    public void driveChanged(String newPath) {
        handleChangeDirOnClient(newPath);
    }

    /**
     * Обработка перемещения по директориям
     * Если кликнули 2 раза на элементе таблицы FileInfoView и это директория,
     * то происходит переход в эту директорию
     * @param controller контроллер стороны, который запросил изменение директории
     * @param newPath новый путь
     */
    public void changeDir(SideController controller, String newPath) {
        if(controller.equals(clientSideController)) {
            handleChangeDirOnClient(newPath);
        } else {
            client.changeDir(newPath);
        }
    }

    /**
     * Общая обработка получения размера директории для клиентской и серверной стороны
     * @param controller контроллер в котором запросили размер
     * @param path путь к директории
     */
    public void sizeClicked(SideController controller, String path) {
        if(controller.equals(clientSideController)) {
            handleSizeClickedOnClient(path);
        } else {
            client.viewDirSize(path);
        }
    }

    // ------------------------- Обработка нажатия на кнопках меню ---------------------------
    /**
     * Соединение с сервером (при щелчке на кнопке "Menu" -> "Соединение" -> "Соединиться...")
     */
    public void connectClick() {
        client = new Client(this);
        client.connect();
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
     * @param fileType тип создаваемого файла
     */
    private void createFile(FileType fileType) {
        if(!clientSideController.isTableFocused() && !serverSideController.isTableFocused()) {
            showError("Не выбрано место создания! Выберите место создания файла кликнув на одной из таблиц");
            return;
        }
        TextInputDialog dialog = new TextInputDialog();
        dialog.setHeaderText(fileType == FileType.FILE ? "Введите имя нового файла" : "Введите имя новой папки");
        dialog.showAndWait().ifPresent(newFileName -> {
            if (!newFileName.equals("")) {
                if(clientSideController.isTableFocused()) {
                    handleCreateFileOnClient(newFileName, fileType);
                } else {
                    String newFilePath = serverSideController.getCurrentPath().resolve(newFileName).toString();
                    if(fileType == FileType.FILE) {
                        client.createFile(newFilePath);
                    } else {
                        client.createDir(newFilePath);
                    }
                }
            }
        });
    }

    /**
     * Общая для клиента и сервера логика обработки события кнопки "Переименовать"
     */
    public void renameFile() {
        if(!clientSideController.isTableFocused() && !serverSideController.isTableFocused()) {
            showError("Не выбран ни один файл или выбраны сразу в двух таблицах");
            return;
        }
        SideController controller;
        if(clientSideController.isTableFocused()) {
            controller = clientSideController;
        } else {
            controller = serverSideController;
        }
        FileInfoView selectedFileInfoView = controller.getSelectedItem();

        if(selectedFileInfoView.getFileInfo().equals(FileInfo.PARENT_DIR)) {
            return;
        }

        SideController finalController = controller;

        FileInfo oldFileInfo = selectedFileInfoView.getFileInfo();
        String oldFileName = oldFileInfo.getFileName();

        TextInputDialog dialog = new TextInputDialog();
        dialog.setHeaderText("Введите новое имя");
        dialog.showAndWait().ifPresent(newFileName -> {
            if (!newFileName.equals("") && !newFileName.equals(oldFileName)) {
                if(finalController.equals(serverSideController)) {
                    String oldFilePath = serverSideController.getCurrentPath().resolve(oldFileName).toString();
                    String newFilePath = serverSideController.getCurrentPath().resolve(newFileName).toString();
                    client.renameFile(oldFilePath, newFilePath);
                } else {
                    handleRenameFileOnClient(oldFileInfo, newFileName);
                }
            }
        });
    }

    /**
     * Общая для клиента и сервера логика обработки события нажатия кнопки "Удалить"
     */
    public void deleteFile() {
        if(!clientSideController.isTableFocused() && !serverSideController.isTableFocused()) {
            showError("Не выбран ни один файл или выбраны сразу в двух таблицах");
            return;
        }
        SideController controller;
        if(clientSideController.isTableFocused()) {
            controller = clientSideController;
        } else {
            controller = serverSideController;
        }
        FileInfoView selectedFileInfoView = controller.getSelectedItem();

        if(selectedFileInfoView.getFileInfo().equals(FileInfo.PARENT_DIR)) {
            return;
        }

        FileInfo selectedFileInfo = selectedFileInfoView.getFileInfo();
        String fileType = selectedFileInfo.getType() == FileType.DIR ? "директорию " : "файл ";
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Удалить " + fileType +
                "\"" + selectedFileInfo.getFileName() + "\" ?");
        if(alert.showAndWait().get() == ButtonType.OK) {
            if(controller.equals(serverSideController)) {
                client.deleteFile(serverSideController.getCurrentPath().resolve(selectedFileInfo.getFileName()).toString());
            } else {
                handleDeleteFileOnClient(selectedFileInfoView);
            }
        }
    }

    /**
     * Общая для клиента и сервера логика обработки события кнопки "Копировать"
     */
    public void copyClick() {
        if(!clientSideController.isTableFocused() && !serverSideController.isTableFocused()) {
            showError("Не выбран ни один файл или выбраны сразу в двух таблицах");
            return;
        }
        SideController controller;
        if(clientSideController.isTableFocused()) {
            controller = clientSideController;
        } else {
            controller = serverSideController;
        }
        FileInfoView selectedFileInfoView = controller.getSelectedItem();

        if(selectedFileInfoView.getFileInfo().equals(FileInfo.PARENT_DIR)) {
            return;
        }

        FileInfo selectedFileInfo = selectedFileInfoView.getFileInfo();
        String fileName = selectedFileInfo.getFileName();
        String fileType = selectedFileInfo.getType() == FileType.DIR ? "директорию " : "файл ";
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Скопировать " + fileType +
               "\"" + fileName + "\"" + (controller.equals(clientSideController) ? " в хранилище?" :  " из хранилища?"));
        if(alert.showAndWait().get() == ButtonType.OK) {
            startProgress();
            Path currentPath = clientSideController.getCurrentPath();
            if(controller.equals(serverSideController)) {
                client.downloadFile(currentPath, serverSideController.getCurrentPath().resolve(fileName).toString());
            } else {
                client.uploadFile(currentPath.resolve(fileName), serverSideController.getCurrentPath());
            }
        }
    }

    /**
     * Начальное отображение окна с ходом загрузки или скачивания
     */
    private void startProgress() {
        progressStage = new ProgressStage("Копирование файлов");
        progressStage.setMessage("Ожидание готовности сервера...");
        progressStage.show();
    }

    /**
     * Общая для клиента и сервера логика обработки поиска файла
     * @param controller контроллер на котором был инициирован поиск файла
     * @param fileName имя искомого файла
     */
    public void searchFile(SideController controller, String fileName) {
        if(controller.equals(clientSideController)) {
            try {
                List<FileInfoView> fileInfoViewList = findFiles(fileName);
                if(fileInfoViewList.size() > 0) {
                    SearchStage stage = new SearchStage();
                    stage.addItems(fileInfoViewList);
                    stage.show();
                } else {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Ничего не найдено");
                    alert.showAndWait();
                }
            } catch (IOException ex) {
                showError(ex.getMessage());
            }
        } else {
            if(isSign) {
                client.searchFile(serverSideController.getCurrentPath().toString(), fileName);
            }
        }
    }

    // ---------------------  Обработка на стороне клиента команд:  ----------------------------------
    // 1. Change dir
    // 2. Create file
    // 3. Rename file
    // 4. Delete file
    // 5. Get directory size
    // 6. Search file

    /**
     * Обработка изменения текущей директории на стороне клиента
     * @param pathString новый путь
     */
    private void handleChangeDirOnClient(String pathString) {
        Path newPath = Paths.get(pathString);
        List<FileInfoView> listFileInfoView = getListFileInfoViewForClient(newPath);
        clientSideController.setCurrentPath(newPath.toString());
        clientSideController.invalidateTable(listFileInfoView);
    }

    /**
     * Создание нового файла (директории) на клиенте
     * @param fileName имя нового файла (директории)
     * @param type тип файла (регулярный файл или директория)
     */
    private void handleCreateFileOnClient(String fileName, FileType type) {
        try {
            Path path;
            Path currentPathOnClient = clientSideController.getCurrentPath();
            if(type == FileType.FILE) {
                path = Files.createFile(currentPathOnClient.resolve(fileName));
            } else {
                path = Files.createDirectory(currentPathOnClient.resolve(fileName));
            }
            FileInfoView fileInfoView = getFromPath(path);
            clientSideController.add(fileInfoView);
        } catch (IOException ex) {
            showError(ex.getMessage());
        }
    }

    /**
     * Переименовывание файла (директории) на клиенте
     * @param oldFileInfo старое имя файла
     * @param newFileName новое имя файла
     */
    private void handleRenameFileOnClient(FileInfo oldFileInfo, String newFileName) {
        try {
            Path clientCurrentPath = clientSideController.getCurrentPath();
            Files.move(clientCurrentPath.resolve(oldFileInfo.getFileName()),
                    clientCurrentPath.resolve(newFileName));
            oldFileInfo.setFileName(newFileName);
            clientSideController.sortAndRefreshTable();
        } catch (IOException e) {
            showError(e.getMessage());
        }
    }

    /**
     * Удаление файла на клиенте
     * @param fileInfoView элемент из клиентской таблицы
     */
    private void handleDeleteFileOnClient(FileInfoView fileInfoView) {
        Path filePath = clientSideController.getCurrentPath().resolve(fileInfoView.getFileInfo().getFileName());
        try {
            if(Files.isDirectory(filePath)) {
                recursiveDeleteDirectoryOnClient(filePath);
            } else {
                Files.delete(filePath);
            }
        } catch (IOException e) {
            showError(e.getMessage());
        }
        clientSideController.remove(fileInfoView);
    }

    /**
     * Рекурсивное удаление директории на клиенте
     * @param path путь к удаляемой директори
     * @throws IOException при возникновении ошибок, возникающих при удалении
     */
    private void recursiveDeleteDirectoryOnClient(Path path) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
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
    }

    /**
     * Обработка получения размера директории на стороне клиента
     * @param dirPath путь к директории
     */
    private void handleSizeClickedOnClient(String dirPath) {
        Path path = Paths.get(dirPath);
        long size = getDirSizeOnClient(path);
        FileInfoView fileInfoView = clientSideController.getByFileName(path.getFileName().toString());
        if(fileInfoView != null) {
            fileInfoView.getFileInfo().setSize(size);
            clientSideController.refresh();
        }
    }

    /**
     * Вычисление размера директории на клиентской стороне
     * @param path путь к директории
     * @return размер директории
     */
    private long getDirSizeOnClient(Path path) {
        final long[] result = {0};
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    result[0] += file.toFile().length();
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ex) {
            showError(ex.getMessage());
            result[0] = 0;
        }
        return result[0];
    }

    /**
     * Поиск файла на клиенте. При поиске выбираются любые файлы имена которых содержат искомое имя файла
     * @param fileName имя искомого файла
     * @return список найденных файлов типа List<FileInfo>
     * @throws IOException ошибки при поиске файла
     */
    private List<FileInfoView> findFiles(String fileName) throws IOException {
        List<FileInfoView> result = new LinkedList<>();
        Path startPath = clientSideController.getCurrentPath();
        Files.walkFileTree(startPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if(!dir.equals(startPath) && dir.getFileName().toString().contains(fileName)) {
                    FileInfoView fileInfoView = getFromPath(dir);
                    fileInfoView.getFileInfo().setFileName(dir.toString());
                    result.add(fileInfoView);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if(file.getFileName().toString().contains(fileName)) {
                    FileInfoView fileInfoView = getFromPath(file);
                    fileInfoView.getFileInfo().setFileName(file.toString());
                    result.add(fileInfoView);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return result;
    }

    // ------------------ Обработка ответов от сервера ----------------

    /**
     * Происходит после успешной установки соединения с сервером
     * Открывается диалоговое окно аутентификации типа ConnectionStage с текстовыми полями для ввода
     * логина и пароля пользователя. Если клиент нажал кнопку Cancel, соединение с сервером разрывается
     */
    public void channelActivated() {
        Platform.runLater(() -> {
            try {
                connectionStage = new ConnectionStage();
                connectionStage.setOwner(newFile.getScene().getWindow());
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

    /**
     * Происходит при успешном прохождении процедуры аутентификации или авторизации
     * @param currentDir текущая директория на сервере
     * @param files список файлов типа FileInfo в текущей директории
     */
    public void clientSigned(String currentDir, List<FileInfo> files) {
        menuItemConnect.setDisable(true);
        menuItemDisconnect.setDisable(false);
        isSign = true;
        currentDirChanged(currentDir, files);
    }

    /**
     * Происходит при успешном изменении текущей директории на сервере
     * @param newDirPath путь новой директории
     * @param files список файлов на сервере по новой директории
     */
    public void currentDirChanged(String newDirPath, List<FileInfo> files) {
        Platform.runLater(() -> {
            String currentDirForView = newDirPath;
            if(newDirPath.equals(ApplicationUtil.SERVER_USER_ROOT_SYMBOL)) {
                currentDirForView = ApplicationUtil.SERVER_USER_ROOT_SYMBOL + File.separator;
            }
            serverSideController.setCurrentPath(currentDirForView);
            serverSideController.invalidateTable(getFromFileInfo(newDirPath, files));
        });
    }

    /**
     * Происходит при успешном создании файла на сервере
     * @param fileInfo информация о новом созданном файле
     */
    public void fileCreated(FileInfo fileInfo) {
        Platform.runLater(() -> {
            FileInfoView fileInfoView = new FileInfoView(fileInfo);
            serverSideController.add(fileInfoView);
        });
    }

    /**
     * Происходит при успешном переименовании файла на сервере
     * @param oldFilePath путь к файлу со старым названием
     * @param newFilePath путь к файлу с новым названием
     */
    public void fileRenamed(String oldFilePath, String newFilePath) {
        Platform.runLater(() -> {
            FileInfoView fileInfoView = serverSideController.getByFileName(new File(oldFilePath).getName());
            fileInfoView.getFileInfo().setFileName(new File(newFilePath).getName());
            serverSideController.sortAndRefreshTable();
        });
    }

    /**
     * Прорисходит при успешном удалении файла на сервере
     * @param deletedFilePath путь к удаленному файлу
     */
    public void fileDeleted(String deletedFilePath) {
        Platform.runLater(() -> {
            FileInfoView fileInfoView = serverSideController.getByFileName(Paths.get(deletedFilePath).getFileName().toString());
            serverSideController.remove(fileInfoView);
        });
    }

    /**
     * Отображение хода загрузки (upload) файлов на удаленный сервер
     * @param percent процент выгрузки (от 0.0 до 1.0)
     */
    public void progressUpload(double percent, String fileName) {
        Platform.runLater(() -> {
            progressStage.setProgress(percent);
            progressStage.setFileName(fileName);
            progressStage.setMessage("Копирование файлов...");
        });
    }

    public void uploadDone(String currentDir, List<FileInfo> fileInfoList) {
        Platform.runLater(() -> {
            progressStage.close();
            if(serverSideController.getCurrentPath().toString().equals(currentDir)) {
                List<FileInfoView> fileInfoViewList = getFromFileInfo(currentDir, fileInfoList);
                serverSideController.invalidateTable(fileInfoViewList);
            }
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                    "Копирование файлов завершено");
            alert.showAndWait();
        });
    }

    /**
     * Отображение хода загрузки файлов из сервера
     * @param percent процент выгрузки (от 0.0 до 1.0)
     */
    public void progressDownload(double percent, String filePath) {
        Platform.runLater(() -> {
            progressStage.setProgress(percent);
            progressStage.setFileName(filePath);
            progressStage.setMessage("Копирование файлов...");
        });
    }

    @Override
    public void downloadDone(String downloadingPath) {
        Platform.runLater(() -> {
            progressStage.close();
            if(clientSideController.getCurrentPath().toString().equals(downloadingPath)) {
                List<FileInfoView> fileInfoViewList = getListFileInfoViewForClient(clientSideController.getCurrentPath());
                clientSideController.invalidateTable(fileInfoViewList);
            }
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                    "Копирование файлов завершено");
            alert.showAndWait();
        });
    }

    /**
     * Обработка полученного списка файлов от сервера по запросу на поиск файла
     * @param files список найденных файлов типа FileInfo
     */
    public void foundedFilesReceived(List<FileInfo> files) {
        Platform.runLater(() -> {
            SearchStage search = new SearchStage();
            List<FileInfoView> fileInfoViewList = files.stream()
                    .map(fileInfo -> new FileInfoView(fileInfo))
                    .collect(Collectors.toList());
            search.addItems(fileInfoViewList);
            search.show();
        });
    }

    /**
     * Отображение размера директории в таблице стороны сервера
     * @param fileInfo информация о директории
     * @param size размер директории
     */
    public void viewDirSizeOnServer(FileInfo fileInfo, long size) {
        Platform.runLater(() -> {
            FileInfoView fileInfoView = serverSideController.getByFileName(fileInfo.getFileName());
            fileInfoView.getFileInfo().setSize(size);
            serverSideController.refresh();
        });
    }

    /**
     * Обработка полученных ошибок от сервера. Сообщение с ошибкой отображается в окне
     * @param message сообщение об ошибке
     */
    public void errorReceived(String message) {
        Platform.runLater(() -> {
            showError(message);
            if(client != null && !isSign) {
                channelActivated();
            }
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

    // ---------------------------------------------------------------------------------------------

    /**
     * Получение списка FileInfoView из пути к директории
     * @param newPath путь к файлу
     */
    private List<FileInfoView> getListFileInfoViewForClient(Path newPath) {
        List<FileInfoView> result = new LinkedList<>();
        if(newPath.getParent() != null) {
            result.add(new FileInfoView(FileInfo.PARENT_DIR));
        }
        File[] files = newPath.toFile().listFiles();
        if(files != null) {
            for(File file : files) {
                result.add(getFromPath(file.toPath()));
            }
        }
        return result;
    }

    /**
     * Получение FileInfoView из пути к файлу
     * @param path путь к файлу
     * @return объект типа FileInfoView
     */
    private FileInfoView getFromPath(Path path) {
        FileInfoView fileInfoView;
        try {
            BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);
            FileType type = attr.isDirectory() ? FileType.DIR : FileType.FILE;
            String fileName = path.getFileName().toString();
            long size = type == FileType.DIR ? -1L : attr.size();
            long lastModified = attr.lastModifiedTime().toMillis();
            long createDate = attr.creationTime().toMillis();
            FileInfo fileInfo = new FileInfo(type, fileName, size, lastModified, createDate);
            fileInfoView = new FileInfoView(fileInfo);
        } catch (IOException ex) {
            FileInfo fileInfo = new FileInfo(FileType.FILE, path.getFileName().toString(), -1L, 0L, 0L);
            fileInfoView = new FileInfoView(fileInfo);
        }
        return fileInfoView;
    }

    /**
     * Преобразование из List<FileInfo>, полученного от сервера в List<FileInfoView>
     *     для представления в таблице
     * @param newDirPath новая текущая директория на сервере
     * @param listFileInfo список информации о файлах на сервере
     * @return список преобразованных FileInfo
     */
    private List<FileInfoView> getFromFileInfo(String newDirPath, List<FileInfo> listFileInfo) {
        List<FileInfoView> result = new LinkedList<>();
        if(!newDirPath.equals(ApplicationUtil.SERVER_USER_ROOT_SYMBOL)) {
            result.add(new FileInfoView(FileInfo.PARENT_DIR));
        }
        for(FileInfo fileInfo : listFileInfo) {
            result.add(new FileInfoView(fileInfo));
        }
        return result;
    }


    /**
     * Закрытие соединения и выход из программы
     */
    public void closeConnection() {
        closeChannel();
        Platform.exit();
    }

    /**
     * Закрытие канала
     */
    private void closeChannel() {
        if(client != null) {
            client.disconnect();
        }
    }

    /**
     * Происходит после нажатия на кнопке "Отсоединиться от сервера"
     */
    public void disconnectClick() {
        closeChannel();
        setToInitialState();
        showInfo("Соединение закрыто");
    }

    /**
     * Приведение в исходное состояние
     */
    private void setToInitialState() {
        isSign = false;
        menuItemConnect.setDisable(false);
        menuItemDisconnect.setDisable(true);
        serverSideController.clear();
        serverSideController.setCurrentPath("");
    }

    /**
     * Обработка ошибок, возникающих в канале
     * @param cause исключение в канале
     */
    @Override
    public void exceptionCaught(Throwable cause) {
        Platform.runLater(() -> {
            if(cause instanceof ConnectException) {
                showError("Соединение с сервером отсутствует");
            } else {
                if(connectionStage.isShowing()) {
                    connectionStage.close();
                }
                showInfo("Соединение с сервером потеряно");
            }
            setToInitialState();
        });
    }
}
