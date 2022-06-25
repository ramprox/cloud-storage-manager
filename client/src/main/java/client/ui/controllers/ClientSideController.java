package client.ui.controllers;

import client.events.DownloadDoneEvent;
import client.ui.interfaces.SideEventsListener;
import client.ui.model.FileInfoView;
import client.ui.stages.SearchStage;
import interop.dto.fileinfo.FileInfo;
import interop.dto.fileinfo.FileType;
import interop.service.FileInfoService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import org.springframework.context.event.EventListener;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class ClientSideController extends AbstractSideController implements SideEventsListener {

    private SimpWatchService simpWatchService;

    protected ClientSideController(FileInfoService fileInfoService) {
        super(fileInfoService);
    }

    public List<FileInfoView> getSelectedItems() {
        return sideController.getSelectedItems();
    }

    public String getCurrentPath() {
        return sideController.getCurrentPath();
    }

    public void close() {
        simpWatchService.close();
    }

    @FXML
    private void initialize() {
        sideController.setSideEventProcessable(this);
        List<String> roots = Stream.of(File.listRoots())
                .map(File::toString)
                .collect(Collectors.toList());
        sideController.setDrives(roots);
        String startDir = System.getProperty("user.home");
        Path currentClientPath = Paths.get(startDir);
        sideController.selectDrive(currentClientPath.getRoot().toString());
        sideController.setCurrentPath(startDir);
        sideController.invalidateTable(getListFileInfoView(currentClientPath));

        simpWatchService = new SimpWatchService(currentClientPath);
    }

    private void externalFileChanged(String path, String fileName, WatchEvent.Kind<?> event) {
        if(event.toString().equals("ENTRY_CREATE")) {
            Path newFilePath = Paths.get(path, fileName);
            FileInfo fileInfo = fileInfoService.getFileInfo(newFilePath);
            fileInfo.setFileName(fileName);
            FileInfoView fileInfoView = new FileInfoView(fileInfo);
            sideController.add(fileInfoView);
        } else if(event.toString().equals("ENTRY_DELETE")) {
            sideController.remove(fileName);
        }
    }

    @Override
    protected void internalCreateFile(FileType type, String fileName) {
        try {
            Path newFilePath = Paths.get(sideController.getCurrentPath(), fileName);
            if (type == FileType.FILE) {
                Files.createFile(newFilePath);
            } else {
                Files.createDirectory(newFilePath);
            }
            FileInfo fileInfo = fileInfoService.getFileInfo(newFilePath);
            fileInfo.setFileName(fileName);
            FileInfoView fileInfoView = new FileInfoView(fileInfo);
            sideController.add(fileInfoView);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    protected void internalRename(String oldFileName, String newFileName) {
        try {
            Path clientCurrentPath = Paths.get(sideController.getCurrentPath());
            Path oldPath = clientCurrentPath.resolve(oldFileName);
            Path newPath = clientCurrentPath.resolve(newFileName);
            Files.move(oldPath, newPath);
            sideController.getSelectedItem().getFileInfo().setFileName(newFileName);
            sideController.sortAndRefreshTable();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void internalChangeDir(Path newPath) {
        List<FileInfoView> listFileInfoView = getListFileInfoView(newPath);
        sideController.setCurrentPath(newPath.toString());
        sideController.invalidateTable(listFileInfoView);
        simpWatchService.register(newPath);
    }

    @Override
    protected void internalDelete(FileInfoView selectedFileInfoView) {
        try {
            FileInfo selectedFileInfo = selectedFileInfoView.getFileInfo();
            Path path = Paths.get(sideController.getCurrentPath(), selectedFileInfo.getFileName());
            if (selectedFileInfo.getType() == FileType.FILE) {
                Files.delete(path);
            } else {
                recursiveDeleteDirectory(path);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        sideController.remove(selectedFileInfoView);
    }

    /**
     * Рекурсивное удаление директории
     *
     * @param path путь к удаляемой директори
     * @throws IOException при возникновении ошибок, возникающих при удалении
     */
    private void recursiveDeleteDirectory(Path path) throws IOException {
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

    @Override
    public void driveChanged(String drivePath) {
        Path newPath = Paths.get(drivePath);
        List<FileInfoView> listFileInfoView = getListFileInfoView(newPath);
        sideController.setCurrentPath(newPath.toString());
        sideController.invalidateTable(listFileInfoView);
    }

    @Override
    public void sizeClicked(Path path) {
        long size = getDirSize(path);
        FileInfoView fileInfoView = sideController.getByFileName(path.getFileName().toString());
        fileInfoView.getFileInfo().setSize(size);
        sideController.refresh();
    }

    /**
     * Вычисление размера директории
     *
     * @param path путь к директории
     * @return размер директории
     */
    private long getDirSize(Path path) {
        final long[] result = {0};
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    result[0] += Files.size(file);
                    return FileVisitResult.CONTINUE;
                }
            });
            return result[0];
        } catch (IOException ex) {
            throw new RuntimeException("Не могу вычислить размер файлов. " + ex);
        }
    }

    @Override
    public void searchFile(String fileName) {
        try {
            Path startPath = Paths.get(sideController.getCurrentPath());
            List<FileInfoView> fileInfoViewList = findFiles(startPath, fileName);
            if (fileInfoViewList.size() > 0) {
                SearchStage stage = getSearchStage();
                stage.addItems(fileInfoViewList);
                stage.show();
            } else {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Ничего не найдено");
                alert.showAndWait();
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Поиск файла
     */
    private List<FileInfoView> findFiles(Path startPath, String fileName) throws IOException {
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + fileName);
        try (Stream<Path> paths = Files.walk(startPath)) {
            return paths.filter(path -> matcher.matches(path.getFileName())).map(path -> {
                FileInfo fileInfo = fileInfoService.getFileInfo(path);
                return new FileInfoView(fileInfo);
            }).collect(Collectors.toList());
        }
    }

    /**
     * Получение списка FileInfoView из пути к директории
     *
     * @param newPath путь к файлу
     */
    protected List<FileInfoView> getListFileInfoView(Path newPath) {
        try (Stream<Path> files = Files.list(newPath)) {
            List<FileInfoView> list = files.map(path -> {
                FileInfo fileInfo = fileInfoService.getFileInfo(path);
                fileInfo.setFileName(path.getFileName().toString());
                return new FileInfoView(fileInfo);
            }).collect(Collectors.toList());
            if (newPath.getParent() != null) {
                list.add(new FileInfoView(FileInfo.PARENT_DIR));
            }
            return list;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @EventListener
    public void downloadDoneListen(DownloadDoneEvent event) {
        Path destination = Paths.get(event.getDestination());
        Path currentPath = Paths.get(getCurrentPath());
        if (!destination.relativize(currentPath).startsWith("..")) {
            List<FileInfoView> listFileInfoView = getListFileInfoView(currentPath);
            sideController.invalidateTable(listFileInfoView);
        }
    }

    private class SimpWatchService {

        private final ExecutorService executorService = Executors.newSingleThreadExecutor();

        private final WatchService watchService;

        private WatchKey key;

        private Path path;

        public SimpWatchService(Path path) {
            try {
                watchService = FileSystems.getDefault().newWatchService();
                register(path);
                executorService.execute(this::watch);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void close() {
            executorService.shutdownNow();
        }

        public void register(Path path) {
            try {
                if(key != null) {
                    key.cancel();
                }
                this.path = path;
                path.register(watchService,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_DELETE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void watch() {
            try {
                while(!Thread.currentThread().isInterrupted()) {
                    key = watchService.take();
                    if (key.isValid() && key.watchable().equals(this.path)) {
                        for (WatchEvent<?> event : key.pollEvents()) {
                            externalFileChanged(key.watchable().toString(),
                                    event.context().toString(), event.kind());
                        }
                    }
                }
            } catch (InterruptedException e) {
                System.out.println("WatchService остановлен");
            }
        }
    }
}
