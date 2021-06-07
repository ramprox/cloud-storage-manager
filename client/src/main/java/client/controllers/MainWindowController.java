package client.controllers;

import client.stages.*;
import client.interfaces.Presentable;
import client.utils.tablecellfactories.*;
import client.model.*;
import client.network.Client;
import client.utils.Comparators;
import com.sun.javafx.scene.control.skin.TableColumnHeader;
import interop.model.fileinfo.*;
import javafx.application.Platform;
import javafx.beans.*;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.*;
import javafx.event.*;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.*;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * Главное окно приложения
 */
public class MainWindowController implements Presentable {
    @FXML
    private TableView<FileInfoView> clientTable;
    @FXML
    private TableColumn<FileInfoView, FileNameType> clientFileName;
    @FXML
    private TableColumn<FileInfoView, SimpleLongProperty> clientFileSize;
    @FXML
    private TableColumn<FileInfoView, SimpleStringProperty> clientFileDate;
    @FXML
    private TableColumn<FileInfoView, SimpleStringProperty> clientFileCreateDate;
    @FXML
    private ComboBox<File> clientDrives;
    @FXML
    private Label clientPath;
    @FXML
    private MenuItem menuItemConnect;
    @FXML
    private MenuItem menuItemDisconnect;
    @FXML
    private TableView<FileInfoView> serverTable;
    @FXML
    private TableColumn<FileInfoView, FileNameType> serverFileName;
    @FXML
    private TableColumn<FileInfoView, SimpleLongProperty> serverFileSize;
    @FXML
    private TableColumn<FileInfoView, SimpleStringProperty> serverFileDate;
    @FXML
    private TableColumn<FileInfoView, SimpleStringProperty> serverFileCreateDate;
    @FXML
    private Label serverPath;
    @FXML
    private Button newFile;
    @FXML
    private Button newDir;
    @FXML
    private Button rename;
    @FXML
    private Button copy;
    @FXML
    private Button delete;

    private Client client;                               // объект, через который происходит подключение к серверу
    private User user;
    private static Path currentClientDir;                // текущая директория на стороне клиента
    private ProgressStage progressStage;                 // окно с отображением хода загрузки и выгрузки файлов
    public static final String parentDir = "[ . . ]";   // строка для отображения родительской директории
    private ConnectionStage connectionStage;                    // диалоговое окно аутентификации

    // по умолчанию сортировка для обеих таблиц устанавливается по имени файла в порядке возрастания
    private Comparator<FileInfoView> clientComparator = Comparators.comparatorByName(TableColumn.SortType.ASCENDING);
    private Comparator<FileInfoView> serverComparator = clientComparator;


    /**
     * Происходит установка свойств таблиц и колонок клиента и сервера.
     * По умолчанию текущий путь для клиента устанавливается его домашней директорией
     * По этой домашней директории получается список файлов и заносится в таблицу клиента
     * @throws IOException может возникнуть при ошибках чтения файлов из домашней диерктории клиента
     */
    public void init() throws IOException {
        clientTable.setOnMouseClicked(this::clientTableMouseClicked);
        clientFileName.setCellValueFactory(new PropertyValueFactory<>("fileNameType"));
        clientFileSize.setCellValueFactory(new PropertyValueFactory<>("size"));
        clientFileDate.setCellValueFactory(new PropertyValueFactory<>("lastModified"));
        clientFileCreateDate.setCellValueFactory(new PropertyValueFactory<>("fileCreateDate"));
        clientFileName.setCellFactory(new FileNameCellFactory<>(this::viewClientDirSize));
        clientFileDate.setCellFactory(new FileCreateCellFactory<>());
        clientFileCreateDate.setCellFactory(new FileCreateCellFactory<>());
        clientFileSize.setCellFactory(new FileSizeCellFactory<>());

        serverTable.setOnMouseClicked(this::serverTableMouseClicked);
        serverFileName.setCellValueFactory(new PropertyValueFactory<>("fileNameType"));
        serverFileSize.setCellValueFactory(new PropertyValueFactory<>("size"));
        serverFileDate.setCellValueFactory(new PropertyValueFactory<>("lastModified"));
        serverFileCreateDate.setCellValueFactory(new PropertyValueFactory<>("fileCreateDate"));
        serverFileName.setCellFactory(new FileNameCellFactory<>(this::viewServerDirSize));
        serverFileDate.setCellFactory(new FileCreateCellFactory<>());
        serverFileCreateDate.setCellFactory(new FileCreateCellFactory<>());
        serverFileSize.setCellFactory(new FileSizeCellFactory<>());

        currentClientDir = Paths.get(System.getProperty("user.home"));
        invalidateClientTable();
        File[] paths = File.listRoots();
        clientDrives.setItems(FXCollections.observableArrayList(paths));
        clientDrives.getSelectionModel().select(currentClientDir.getRoot().toFile());
        clientPath.setText(currentClientDir.toString());
        addShortcuts();
    }

    /**
     * Добавление горячих клавиш для пунктов меню
     */
    private void addShortcuts() {
        newFile.getScene().getAccelerators().put(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN), () -> newFile.fire());
        newDir.getScene().getAccelerators().put(new KeyCodeCombination(KeyCode.D, KeyCombination.CONTROL_DOWN), () -> newDir.fire());
        rename.getScene().getAccelerators().put(new KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_DOWN), () -> rename.fire());
        copy.getScene().getAccelerators().put(new KeyCodeCombination(KeyCode.F5), () -> copy.fire());
        delete.getScene().getAccelerators().put(new KeyCodeCombination(KeyCode.DELETE), () -> delete.fire());
    }

    /**
     * Обработка события клика левой кнопкой мыши на таблице клиента
     * Если кликнули на одном из TableColumnHeader, то происходит сортировка
     * Если кликнули 2 раза на элементе таблицы FileInfoView и это директория,
     * то происходит переход в эту директорию
     * @param event событие нажатия кнопки мыши
     */
    private void clientTableMouseClicked(MouseEvent event) {
        if(event.getButton().equals(MouseButton.PRIMARY)) {
            EventTarget target = event.getTarget();
            if(target instanceof TableColumnHeader) {
                handleClickOnClientTableColumnHeader((TableColumnHeader)target);
                return;
            }
            if(event.getClickCount() == 2) {
                FileInfoView fileInfo = clientTable.getSelectionModel().getSelectedItem();
                if (fileInfo != null) {
                    if(fileInfo.getName().equals(parentDir)) {
                        changeDirToParentOnClient();
                    } else if(fileInfo.getType().equals(FileType.DIR)) {
                        changeDirOnClient(fileInfo.getName());
                    }
                }
            }
        }
    }

    /**
     * Обработка нажатой кнопки мыши на заголовке колонки TableColumnHeader стороны клиента
     * Происходит сортировка таблицы в зависимости от того, на заголовок какой колонки
     * нажали мышью
     * @param header заголовок колонки типа TableColumnHeader
     */
    private void handleClickOnClientTableColumnHeader(TableColumnHeader header) {
        if(header.getTableColumn().equals(clientFileName)) {
            clientComparator = Comparators.comparatorByName(clientFileName.getSortType());
        } else if(header.getTableColumn().equals(clientFileSize)) {
            clientComparator = Comparators.comparatorBySize(clientFileSize.getSortType());
        } else if(header.getTableColumn().equals(clientFileDate)) {
            clientComparator = Comparators.comparatorByDate(clientFileDate.getSortType());
        } else if(header.getTableColumn().equals(clientFileCreateDate)) {
            clientComparator = Comparators.comparatorByCreateDate(clientFileCreateDate.getSortType());
        }
        FXCollections.sort(clientTable.getItems(), clientComparator);
        clientTable.refresh();
    }

    /**
     * Обработка щелчка левой кнопки мыши по таблице стороны сервера
     * @param event событие щелчка мыши
     */
    private void serverTableMouseClicked(MouseEvent event) {
        if(event.getButton().equals(MouseButton.PRIMARY)) {
            EventTarget target = event.getTarget();
            if(target instanceof TableColumnHeader) {
                handleClickOnServerTableColumnHeader((TableColumnHeader)target);
                return;
            }
            if(event.getClickCount() == 2) {
                FileInfoView fileInfo = serverTable.getSelectionModel().getSelectedItem();
                if(fileInfo != null) {
                    if(fileInfo.getType().equals(FileType.DIR)) {
                        String fileName = fileInfo.getName();
                        String path;
                        if(fileName.equals(parentDir)) {
                            path = user.getCurrentDir().getParent().toString();
                        } else {
                            path = user.getCurrentDir().resolve(fileName).toString();
                        }
                        client.changeDir(path);
                    }
                }
            }
        }
    }

    /**
     * Обработка нажатой кнопки мыши на заголовке колонки TableColumnHeader таблицы стороны сервера
     * Происходит сортировка таблицы в зависимости от того, на заголовок какой колонки
     * нажали мышью
     * @param header заголовок колонки типа TableColumnHeader
     */
    private void handleClickOnServerTableColumnHeader(TableColumnHeader header) {
        if(header.getTableColumn().equals(serverFileName)) {
            serverComparator = Comparators.comparatorByName(serverFileName.getSortType());
        } else if(header.getTableColumn().equals(serverFileSize)) {
            serverComparator = Comparators.comparatorBySize(serverFileSize.getSortType());
        } else if(header.getTableColumn().equals(serverFileDate)) {
            serverComparator = Comparators.comparatorByDate(serverFileDate.getSortType());
        } else if(header.getTableColumn().equals(serverFileCreateDate)) {
            serverComparator = Comparators.comparatorByCreateDate(serverFileCreateDate.getSortType());
        }
        FXCollections.sort(serverTable.getItems(), serverComparator);
        serverTable.refresh();
    }

    /**
     * Соединение с сервером
     */
    public void connectClick() {
        client = new Client(this);
        client.connect();
    }

    /**
     * обновление клиентской таблицы
     * @throws IOException возникает при
     */
    private void invalidateClientTable() throws IOException {
        clientTable.getItems().clear();
        if(currentClientDir.getParent() != null) {
            clientTable.getItems().add(getFromFile(new File(parentDir)));
        }
        File[] files = currentClientDir.toFile().listFiles();
        if(files != null) {
            for(File file : files) {
                FileInfoView tableFile = getFromFile(file);
                clientTable.getItems().add(tableFile);
            }
        }
        FXCollections.sort(clientTable.getItems(), clientComparator);
        clientTable.refresh();
    }

    /**
     * Происходит после изменения корневого диска на стороне клиента
     */
    public void clientDriveChanged() {
        File file = clientDrives.getSelectionModel().getSelectedItem();
        if(file != null) {
            changeDirOnClient(file.toString());
        }
    }


    // ---------------------- Обрабока команд на стороне клиента ---------------------
    /**
     * Метод, отвечающий за создание файла (директории) на стороне клиента
     * @param fileName имя файла
     * @param fileType тип файла (регулярный файл или директория)
     */
    private void createFileOnClient(String fileName, FileType fileType) {
        try {
            Path path;
            if(fileType == FileType.FILE) {
                path = Files.createFile(currentClientDir.resolve(fileName));
            } else {
                path = Files.createDirectory(currentClientDir.resolve(fileName));
            }
            File file = path.toFile();
            FileInfoView fileInfoView = getFromFile(file);
            clientTable.getItems().add(fileInfoView);
            invalidateClientTable();
        } catch (IOException ex) {
            showError(ex.getMessage());
        }
    }

    /**
     * Переход на стороне клиента к родительской директории
     */
    private void changeDirToParentOnClient() {
        Path parentPath = currentClientDir.getParent();
        if(parentPath != null) {
            changeDirOnClient(parentPath.toString());
        }
    }

    /**
     * Изменение текущей директории на клиенте
     * @param path новый путь на клиенте
     */
    private void changeDirOnClient(String path) {
        try {
            Path tempPath = Paths.get(path);
            currentClientDir = currentClientDir.resolve(tempPath);
            invalidateClientTable();
            clientPath.setText(currentClientDir.toString());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Обработка переименования файла на клиенте
     * @param oldFileInfo старое название файла
     * @param newFileName новое название файла
     */
    private void renameFileOnClient(FileInfoView oldFileInfo, String newFileName) {
        try {
            String oldFileName = oldFileInfo.getName();
            Files.move(currentClientDir.resolve(oldFileName),
                    currentClientDir.resolve(newFileName)).toFile();
            oldFileInfo.getFileNameType().setFileName(newFileName);
            clientTable.refresh();
        } catch (IOException e) {
            showError(e.getMessage());
        }
    }

    /**
     * Удаление файла на клиенте
     * @param fileInfo элемент из клиенсткой таблицы
     */
    private void deleteFileOnClient(FileInfoView fileInfo) {
        Path filePath = currentClientDir.resolve(fileInfo.getName());
        try {
            if(Files.isDirectory(filePath)) {
                deleteDirectoryOnClient(filePath);
            } else {
                Files.delete(filePath);
            }
        } catch (IOException e) {
            showError(e.getMessage());
        }
        clientTable.getItems().remove(fileInfo);
        FXCollections.sort(clientTable.getItems(), clientComparator);
        clientTable.refresh();
    }

    /**
     * Рекурсивное удаление директории на клиенте
     * @param path путь к удаляемой директори
     * @throws IOException при возникновении ошибок, возникающих при удалении
     */
    private void deleteDirectoryOnClient(Path path) throws IOException {
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
     * Происходит при нажатии клавиши Enter над тектовым полем поиска на стороне клиента
     * @param actionEvent событие Enter на текстовом поле поиска на стороне клиента
     */
    public void searchFileOnClient(ActionEvent actionEvent) {
        try {
            TextField txtField = (TextField)actionEvent.getSource();
            String fileName = txtField.getText();
            List<FileInfo> foundedFiles = findFiles(fileName);
            SearchStage stage = new SearchStage();
            stage.addItems(getListFileInfoView(foundedFiles));
            stage.show();
        } catch (IOException ex) {
            errorReceived(ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Поиск файла на клиенте. При поиске выбираются любые файлы имена которых содержат искомое имя файла
     * @param fileName имя искомого файла
     * @return список найденных файлов типа List<FileInfo>
     * @throws IOException ошибки при поиске файла
     */
    private List<FileInfo> findFiles(String fileName) throws IOException {
        List<FileInfo> result = new LinkedList<>();
        Files.walkFileTree(currentClientDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if(dir.getFileName().toString().contains(fileName)) {
                    result.add(new Directory(dir.toString(), 0L, 0L));
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if(file.getFileName().toString().contains(fileName)) {
                    result.add(new RegularFile(file.toString(), 0L, 0L, 0L));
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return result;
    }

    /**
     * Обработка нажатия кнопки кнотекстного меню "Размер" на клиентской стороне
     */
    private void viewClientDirSize() {
        try {
            FileInfoView selectedFileInfo = clientTable.getSelectionModel().getSelectedItem();
            if(selectedFileInfo != null) {
                String fileName = selectedFileInfo.getName();
                Path path = currentClientDir.resolve(selectedFileInfo.getName());
                long size = getDirSize(path);
                FileInfoView fiv = clientTable.getItems().stream()
                        .filter(fileInfoView -> fileName.equals(fileInfoView.getName()))
                        .findFirst().get();
                fiv.setSize(size);
                clientTable.refresh();
            }
        } catch(IOException ex) {
            showError(ex.getMessage());
        }
    }

    /**
     * Вычисление размера директории на клиентской стороне
     * @param path путь к директории
     * @return размер директории
     * @throws IOException при вычислении размера
     */
    private long getDirSize(Path path) throws IOException {
        final long[] result = {0};
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                result[0] += file.toFile().length();
                return FileVisitResult.CONTINUE;
            }
        });
        return result[0];
    }

    // -----------------------------------------------------------------------------------

    /**
     * Отправка запроса на сервер о получении размера директории
     * @param actionEvent событие нажатия в контекстном меню
     */
    public void searchFileOnServer(ActionEvent actionEvent) {
        TextField searchServerTxtField = (TextField)actionEvent.getSource();
        client.searchFile(searchServerTxtField.getText());
    }

    /**
     * Отправка запроса на сервер для получения размера выбранной директории
     */
    private void viewServerDirSize() {
        FileInfoView selectedFileInfo = serverTable.getSelectionModel().getSelectedItem();
        if(selectedFileInfo != null) {
            client.viewDirSize(selectedFileInfo.getName());
        }
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
                connectionStage.showAndWait();
                ButtonType btnType = connectionStage.getDialogResult();
                if(btnType == ButtonType.OK) {
                    user = new User(connectionStage.getLogin());
                    client.authentication(connectionStage.getLogin(), connectionStage.getPassword());
                } else if(btnType == ButtonType.APPLY) {
                    user = new User(connectionStage.getLogin());
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
        user.setSign(true);
        user.setCurrentDir(Paths.get(currentDir));
        currentDirChanged(currentDir, files);
    }

    /**
     * Происходит при успешном изменении текущей директории на сервере
     * @param newDirPath путь новой директории
     * @param files список файлов на сервере по новой директории
     */
    public void currentDirChanged(String newDirPath, List<FileInfo> files) {
        Platform.runLater(() -> {
            user.setCurrentDir(Paths.get(newDirPath));
            String currentDir = user.getCurrentDir().toString();
            if(!currentDir.equals(user.getLogin())) {
                files.add(new Directory("[ . . ]", -1L, -1L));
            }
            serverPath.setText(user.getPrompt());
            invalidateServerTable(getListFileInfoView(files));
        });
    }

    /**
     * Происходит при успешном создании файла на сервере
     * @param fileInfo информация о новом созданном файле
     */
    public void fileCreated(FileInfo fileInfo) {
        Platform.runLater(() -> {
            serverTable.getItems().add(getTableFileInfo(fileInfo));
            FXCollections.sort(serverTable.getItems(), serverComparator);
            serverTable.refresh();
        });
    }

    /**
     * Происходит при успешном переименовании файла на сервере
     * @param oldFileInfo информация о файле со старым названием
     * @param newFileInfo информация о файле с новым названием
     */
    public void fileRenamed(FileInfo oldFileInfo, FileInfo newFileInfo) {
        Platform.runLater(() -> {
            FileInfoView tableFileInfo = serverTable.getItems()
                    .stream()
                    .filter(tfi -> tfi.getName().equals(oldFileInfo.getFileName()))
                    .findFirst().get();
            tableFileInfo.setName(newFileInfo.getFileName());
            FXCollections.sort(serverTable.getItems(), serverComparator);
            serverTable.refresh();
        });
    }

    /**
     * Прорисходит при успешном удалении файла на сервере
     * @param oldFileInfo информация об удаленном файле
     */
    public void fileDeleted(FileInfo oldFileInfo) {
        Platform.runLater(() -> {
            serverTable.getItems().remove(getTableFileInfo(oldFileInfo));
            FXCollections.sort(serverTable.getItems(), serverComparator);
            serverTable.refresh();
        });
    }

    /**
     * Отображение хода загрузки (upload) файлов на удаленный сервер
     * @param percent процент выгрузки (от 0.0 до 1.0)
     */
    public void progressUpload(double percent) {
        Platform.runLater(() -> {
            progressStage.setProgress(percent);
            if(percent == 1.0) {
                client.changeDir(user.getCurrentDir().toString());
                postLoad();
            } else {
                progressStage.setMessage("Копирование файлов...");
            }
        });
    }

    /**
     * Отображение хода загрузки файлов из сервера
     * @param percent процент выгрузки (от 0.0 до 1.0)
     */
    public void progressDownload(double percent) {
        Platform.runLater(() -> {
            progressStage.setProgress(percent);
            if(percent == 1.0) {
                changeDirOnClient(currentClientDir.toString());
                postLoad();
            } else {
                progressStage.setMessage("Копирование файлов...");
            }
        });
    }

    /**
     * Происходит после загрузки или скачивания файла
     */
    private void postLoad() {
        progressStage.close();
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,"Копирование файлов завершено");
        alert.showAndWait();
    }

    /**
     * Обработка полученного списка файлов от сервера по запросу на поиск файла
     * @param files список найденных файлов типа FileInfo
     */
    public void foundedFilesReceived(List<FileInfo> files) {
        Platform.runLater(() -> {
            try {
                SearchStage search = new SearchStage();
                search.addItems(getListFileInfoView(files));
                search.show();
            } catch (IOException ex) {
                showError("Внутренняя ошибка");
            }
        });
    }

    /**
     * Отображение размера директории в таблице стороны сервера
     * @param fileInfo информация о директории
     * @param size размер директории
     */
    public void viewDirSizeOnServer(FileInfo fileInfo, long size) {
        Platform.runLater(() -> {
            FileInfoView fiv = serverTable.getItems().stream()
                    .filter(fileInfoView -> fileInfo.getFileName().equals(fileInfoView.getName()))
                    .findFirst().get();
            fiv.setSize(size);
            serverTable.refresh();
        });
    }

    /**
     * Обработка полученных ошибок от сервера. Сообщение с ошибкой отображается в окне
     * @param message сообщение об ошибке
     */
    public void errorReceived(String message) {
        Platform.runLater(() -> {
            showError(message);
            if(client != null && !user.isSign()) {
                channelActivated();
            }
        });
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Обновление таблицы серверной стороны
     * @param files обновленный список файлов
     */
    private void invalidateServerTable(List<FileInfoView> files) {
        Platform.runLater(() -> {
            serverTable.getItems().clear();
            serverTable.getItems().addAll(files);
            FXCollections.sort(serverTable.getItems(), serverComparator);
            serverTable.refresh();
        });
    }

    /**
     * Преобразование типа File в FileInfoView
     * @param file преобразуемый тип
     * @return преобразованный тип FileInfoView
     * @throws IOException может возникнуть при проблемах чтения атрибутов файла
     */
    private FileInfoView getFromFile(File file) throws IOException {
        FileInfoView tableFile;
        if(file.getName().equals(parentDir)) {
            FileNameType fileNameType = new FileNameType(parentDir, FileType.DIR);
            tableFile = new FileInfoView(fileNameType,null, null, null);
        } else {
            BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            long createDate = attr.creationTime().toMillis();
            if(file.isDirectory()) {
                FileNameType fileNameType = new FileNameType(file.getName(), FileType.DIR);
                tableFile = new FileInfoView(fileNameType, null, file.lastModified(), createDate);
            } else {
                FileNameType fileNameType = new FileNameType(file.getName(), FileType.FILE);
                tableFile = new FileInfoView(fileNameType, file.length(), file.lastModified(), createDate);
            }
        }
        return tableFile;
    }

    // ----------------------------------------------------------------------------------------------

    /**
     * Отображение окна с сообщением об ошибке
     * @param message сообщение об ошибке
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.showAndWait();
    }

    // ---------------------- Обработка нажатия кнопок меню ----------------------------------------

    /**
     * Происходит при щелчке левой кнопкой мыши по кнопке "Создать файл" в нижней части главного окна
     */
    public void createFileClick() {
        createFileClick(FileType.FILE);
    }

    /**
     * Происходит при щелчке левой кнопкой мыши по кнопке "Создать папку" в нижней части главного окна
     */
    public void createDirClick() {
        createFileClick(FileType.DIR);
    }

    /**
     * Общая логика обработки нажатия кнопки "Создать файл" или "Создать папку"
     * @param fileType тип создаваемого файла
     */
    private void createFileClick(FileType fileType) {
        TableView<FileInfoView> focusedTable = clientTable.isFocused() ? clientTable :
                serverTable.isFocused() ? serverTable : null;
        if(focusedTable != null) {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setHeaderText(fileType == FileType.FILE ? "Создать новый файл" : "Создать новую папку");
            dialog.showAndWait().ifPresent(newFileName -> {
                if (!newFileName.equals("")) {
                    if(focusedTable.equals(clientTable)) {
                        createFileOnClient(newFileName, fileType);
                    } else {
                        client.createFile(newFileName, fileType);
                    }
                }
            });
        }
    }

    /**
     * Общая для клиента и сервера логика обработки события кнопки "Копировать"
     */
    public void copyClick() {
        TableView<FileInfoView> focusedTable = clientTable.isFocused() ? clientTable :
                serverTable.isFocused() ? serverTable : null;
        if(focusedTable != null) {
            FileInfoView selectedFileInfo = focusedTable.getSelectionModel().getSelectedItem();
            if(selectedFileInfo != null) {
                String fileName = selectedFileInfo.getName();
                String fileType = selectedFileInfo.getType() == FileType.DIR ? "директорию " : "файл ";
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Скопировать " + fileType +
                        fileName + (focusedTable.equals(clientTable) ? " в хранилище?" :  " из хранилища?"));
                if(alert.showAndWait().get() == ButtonType.OK) {
                    startProgress();
                    if(focusedTable.equals(serverTable)) {
                        fileName = user.getCurrentDir().resolve(fileName).toString();
                        client.downloadFile(currentClientDir, fileName);
                    } else {
                        client.uploadFile(currentClientDir.resolve(fileName));
                    }
                }
            }
        }
    }

    /**
     * Общая для клиента и сервера логика обработки события кнопки "Переименовать"
     */
    public void renameFile() {
        TableView<FileInfoView> focusedTable = clientTable.isFocused() ? clientTable :
                serverTable.isFocused() ? serverTable : null;
        if(focusedTable != null) {
            FileInfoView selectedFileInfo = focusedTable.getSelectionModel().getSelectedItem();
            if(selectedFileInfo != null) {
                TextInputDialog dialog = new TextInputDialog();
                dialog.setHeaderText("Новое имя");
                dialog.showAndWait().ifPresent(newFileName -> {
                    String oldFileName = selectedFileInfo.getName();
                    if (!newFileName.equals("") && !newFileName.equals(oldFileName)) {
                        if(focusedTable.equals(serverTable)) {
                            client.renameFile(oldFileName, newFileName);
                        } else {
                            renameFileOnClient(selectedFileInfo, newFileName);
                        }
                    }
                });
            }
        }
    }

    /**
     * Общая для клиента и сервера логика обработки события нажатия кнопки "Удалить"
     */
    public void deleteFile() {
        TableView<FileInfoView> focusedTable = clientTable.isFocused() ? clientTable :
                serverTable.isFocused() ? serverTable : null;
        if(focusedTable != null) {
            FileInfoView selectedFileInfo = focusedTable.getSelectionModel().getSelectedItem();
            if(selectedFileInfo != null) {
                String fileType = selectedFileInfo.getType() == FileType.DIR ? "директорию " : "файл ";
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Удалить " + fileType +
                        selectedFileInfo.getName() + "?");
                if(alert.showAndWait().get() == ButtonType.OK) {
                    if(focusedTable.equals(serverTable)) {
                        client.deleteFile(selectedFileInfo.getName());
                    } else {
                        deleteFileOnClient(selectedFileInfo);
                    }
                }
            }
        }
    }

    /**
     * Начальное отображение окна с ходом загрузки или скачивания
     */
    private void startProgress() {
        Platform.runLater(() -> {
            try{
                progressStage = new ProgressStage("Копирование файлов");
                progressStage.setMessage("Ожидание готовности сервера...");
                progressStage.show();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
    }

    /**
     * Преобразование из List<FileInfo> в List<FileInfoView>
     * @param fileInfos преобразуемый список типа List<FileInfo>
     * @return преобразованный список List<FileInfoView>
     */
    private List<FileInfoView> getListFileInfoView(List<FileInfo> fileInfos) {
        List<FileInfoView> result = new LinkedList<>();
        fileInfos.forEach(fi -> result.add(getTableFileInfo(fi)));
        return result;
    }

    /**
     * Преобразование из FileInfo в FileInfoView
     * @param fileInfo преобразуемый объект типа FileInfo
     * @return преобразованный объект типа FileInfoView
     */
    private FileInfoView getTableFileInfo(FileInfo fileInfo) {
        FileType fileType;
        Long size;
        if(fileInfo instanceof RegularFile) {
            fileType = FileType.FILE;
            size = ((RegularFile) fileInfo).getSize();
        } else {
            fileType = FileType.DIR;
            size = null;
        }
        FileNameType fileNameType = new FileNameType(fileInfo.getFileName(), fileType);
        Long createDate = fileInfo.getCreateDate();
        if(createDate == -1) {
            createDate = null;
        }
        Long lastModified = fileInfo.getLastModified();
        if(lastModified == -1) {
            lastModified = null;
        }
        return new FileInfoView(fileNameType, size, lastModified, createDate);
    }

    /**
     * Закрытие соедиенения и выход из программы
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
        menuItemConnect.setDisable(false);
        menuItemDisconnect.setDisable(true);
        serverTable.getItems().clear();
        serverPath.setText("");
    }

    /**
     * Обработка ошибок, возникающих в канале
     * @param cause исключение в канале
     */
    @Override
    public void exceptionCaught(Throwable cause) {
        if(connectionStage.isShowing()) {
            connectionStage.close();
        }
        disconnectClick();
    }

    /**
     * Слушатель изменения фокуса. При попытке перехода фокуса от таблиц к кнопкам расположенным внизу
     * фокус возвращается обратно к таблице
     * @param oldNode элемент от которого переходит фокус
     * @param newNode элемент в который переходит фокус
     */
    public void focusOwnerListener(Node oldNode, Node newNode) {
        if(oldNode != null) {
            if ((oldNode.equals(clientTable) || oldNode.equals(serverTable))
                    && ((newNode.equals(newFile) || newNode.equals(newDir)
                    || newNode.equals(copy) || newNode.equals(rename) || newNode.equals(delete)))) {
                oldNode.requestFocus();
            }
        }
    }
}
