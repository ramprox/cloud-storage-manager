package client.controllers;

import client.interfaces.SideEventsProcessable;
import client.model.FileInfoView;
import client.utils.ApplicationUtil;
import com.sun.javafx.scene.control.skin.TableColumnHeader;
import interop.model.fileinfo.FileInfo;
import interop.model.fileinfo.FileType;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValueBase;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventTarget;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

public class SideController implements Initializable {
    @FXML private TableView<FileInfoView> table;
    @FXML private TableColumn<FileInfoView, FileInfo> nameColumn;
    @FXML private TableColumn<FileInfoView, Long> sizeColumn;
    @FXML private TableColumn<FileInfoView, FileInfo> lastModifiedColumn;
    @FXML private TableColumn<FileInfoView, FileInfo> createDateColumn;
    @FXML private ComboBox<String> drives;
    @FXML private Label currentPath;
    @FXML private TextField searchTextField;

    private static final String PARENT_DIRECTORY = "[ . . ]";
    private static final String SIZE_FORMAT = "%,d байт";
    private static final String pattern = "dd-MM-yyyy HH:mm:ss";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);

    private Comparator<FileInfoView> comparator = FileInfoView.comparatorByName(TableColumn.SortType.ASCENDING);
    private SideEventsProcessable sideEventProcessable;

    public void setDrivesVisible(boolean value) {
        drives.setVisible(value);
    }

    public void setDrives(File[] files) {
        for(File file : files) {
            drives.getItems().add(file.toString());
        }
    }

    public void selectDrive(String drivePath) {
        drives.getSelectionModel().select(drivePath);
    }

    public void setSideEventProcessable(SideEventsProcessable sideEventProcessable) {
        this.sideEventProcessable = sideEventProcessable;
    }

    public void setCurrentPath(String path) {
        currentPath.setText(path);
    }

    public Path getCurrentPath() {
        return Paths.get(currentPath.getText());
    }

    public FileInfoView getSelectedItem() {
        return table.getSelectionModel().getSelectedItem();
    }

    public boolean isTableFocused() {
        return table.isFocused();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        initNameColumn();
        initSizeColumn();
        initLastModifiedColumn();
        initCreateDateColumn();
        table.setRowFactory(param -> {
            final TableRow<FileInfoView> row = new TableRow<>();
            final ContextMenu contextMenu = new ContextMenu();
            final MenuItem sizeMenuItem = new MenuItem("Показать размер");
            contextMenu.getItems().add(sizeMenuItem);
            row.contextMenuProperty().bind(
                    Bindings.when(row.emptyProperty())
                            .then((ContextMenu)null)
                            .otherwise(contextMenu));
            sizeMenuItem.setOnAction(event -> {
                FileInfo fileInfo = table.getSelectionModel().getSelectedItem().getFileInfo();
                if(!fileInfo.equals(FileInfo.PARENT_DIR) && fileInfo.getType() == FileType.DIR) {
                    String dirPath = currentPath.getText() + File.separator + fileInfo.getFileName();
                    if (sideEventProcessable != null) {
                        sideEventProcessable.sizeClicked(SideController.this, dirPath);
                    }
                }
            });
            return row;
        });
    }

    private void initNameColumn() {
        nameColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getFileInfo()));
        nameColumn.setCellFactory(new Callback<TableColumn<FileInfoView, FileInfo>, TableCell<FileInfoView, FileInfo>>() {
            @Override
            public TableCell<FileInfoView, FileInfo> call(TableColumn<FileInfoView, FileInfo> param) {
                final Label label = new Label();
                label.setAlignment(Pos.CENTER_LEFT);
                TableCell<FileInfoView, FileInfo> cell = new TableCell<FileInfoView, FileInfo>() {
                    @Override
                    protected void updateItem(FileInfo item, boolean empty) {
                        if(item != null && !empty) {
                            ImageView imageViewDir;
                            String text;
                            if(item.equals(FileInfo.PARENT_DIR)) {
                                text = PARENT_DIRECTORY;
                                imageViewDir = new ImageView(ApplicationUtil.IMG_PARENT_DIR);
                            } else {
                                text = item.getFileName();
                                if(item.getType() == FileType.DIR) {
                                    imageViewDir = new ImageView(ApplicationUtil.IMG_DIRECTORY);
                                } else {
                                    imageViewDir = new ImageView(ApplicationUtil.IMG_FILE);
                                }
                            }
                            label.setText(text);
                            label.setGraphic(imageViewDir);
                            super.setGraphic(label);
                        }
                    }
                };
                label.prefWidthProperty().bind(cell.widthProperty());
                return cell;
            }
        });
    }

    private void initSizeColumn() {
        sizeColumn.setCellValueFactory(param -> new ObservableValueBase<Long>() {
            @Override
            public Long getValue() {
                return param.getValue().getFileInfo().getSize();
            }
        });
        sizeColumn.setCellFactory(param -> {
            TableCell<FileInfoView, Long> cell = new TableCell<FileInfoView, Long>() {
                @Override
                protected void updateItem(Long item, boolean empty) {
                    if(item != null && !empty) {
                        if(item != -1) {
                            setText(String.format(SIZE_FORMAT, item));
                        } else {
                            setText(null);
                        }
                    }
                }
            };
            cell.setAlignment(Pos.CENTER_RIGHT);
            return cell;
        });
    }

    private void initLastModifiedColumn() {
        lastModifiedColumn.setCellValueFactory(param ->
                new SimpleObjectProperty<>(param.getValue().getFileInfo()));
        lastModifiedColumn.setCellFactory(param -> {
            TableCell<FileInfoView, FileInfo> cell = new TableCell<FileInfoView, FileInfo>() {
                @Override
                protected void updateItem(FileInfo item, boolean empty) {
                    if(item != null) {
                        if(!item.equals(FileInfo.PARENT_DIR)) {
                            LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(item.getLastModifiedDate()),
                                    ZoneId.systemDefault());
                            setText(dateTime.format(formatter));
                        } else {
                            setText(null);
                        }
                    }
                }
            };
            cell.setAlignment(Pos.CENTER);
            return cell;
        });
    }

    private void initCreateDateColumn() {
        createDateColumn.setCellValueFactory(param ->
                new SimpleObjectProperty<>(param.getValue().getFileInfo()));
        createDateColumn.setCellFactory(param -> {
            TableCell<FileInfoView, FileInfo> cell = new TableCell<FileInfoView, FileInfo>() {
                @Override
                protected void updateItem(FileInfo item, boolean empty) {
                    if (item != null) {
                        if (!item.equals(FileInfo.PARENT_DIR)) {
                            LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(item.getCreationDate()),
                                    ZoneId.systemDefault());
                            setText(dateTime.format(formatter));
                        } else {
                            setText(null);
                        }
                    }
                }
            };
            cell.setAlignment(Pos.CENTER);
            return cell;
        });
    }

    public void clickOnTable(MouseEvent mouseEvent) {
        if(mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
            EventTarget target = mouseEvent.getTarget();
            if(target instanceof TableColumnHeader) {
                handleClickOnTableColumnHeader((TableColumnHeader)target);
                return;
            }
            if(mouseEvent.getClickCount() == 2) {
                FileInfoView fileInfoView = table.getSelectionModel().getSelectedItem();
                if(fileInfoView != null) {
                    FileInfo fileInfo = fileInfoView.getFileInfo();
                    if(fileInfo.getType() == FileType.DIR) {
                        String path;
                        if(fileInfo.equals(FileInfo.PARENT_DIR)) {
                            path = getCurrentPath().getParent().toString();
                        } else {
                            path = getCurrentPath().resolve(fileInfo.getFileName()).toString();
                        }
                        if (sideEventProcessable != null) {
                            sideEventProcessable.changeDir(this, path);
                        }
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
    private void handleClickOnTableColumnHeader(TableColumnHeader header) {
        if(header.getTableColumn().equals(nameColumn)) {
            comparator = FileInfoView.comparatorByName(nameColumn.getSortType());
        } else if(header.getTableColumn().equals(sizeColumn)) {
            comparator = FileInfoView.comparatorBySize(sizeColumn.getSortType());
        } else if(header.getTableColumn().equals(lastModifiedColumn)) {
            comparator = FileInfoView.comparatorByLastModifiedDate(lastModifiedColumn.getSortType());
        } else if(header.getTableColumn().equals(createDateColumn)) {
            comparator = FileInfoView.comparatorByCreateDate(createDateColumn.getSortType());
        }
        FXCollections.sort(table.getItems(), comparator);
    }

    public void invalidateTable(List<FileInfoView> fileInfoView1List) {
        table.getItems().clear();
        table.getItems().addAll(fileInfoView1List);
        sortAndRefreshTable();
    }

    public void add(FileInfoView fileInfoView) {
        table.getItems().add(fileInfoView);
        sortAndRefreshTable();
    }

    public void remove(FileInfoView fileInfoView) {
        table.getItems().remove(fileInfoView);
        sortAndRefreshTable();
    }

    public FileInfoView getByFileName(String fileName) {
        return table.getItems().stream()
                .filter(fiv -> fiv.getFileInfo().getFileName().equals(fileName))
                .findFirst().orElse(null);
    }

    public void sortAndRefreshTable() {
        FXCollections.sort(table.getItems(), comparator);
        refresh();
    }

    public void refresh() {
        table.refresh();
    }

    public void clear() {
        table.getItems().clear();
    }

    public void driveChanged() {
        if(sideEventProcessable != null) {
            sideEventProcessable.driveChanged(drives.getSelectionModel().getSelectedItem());
        }
    }

    public void searchFile() {
        String fileName = searchTextField.getText();
        if(!fileName.equals("") && sideEventProcessable != null) {
            sideEventProcessable.searchFile(this, fileName);
        }
    }
}
