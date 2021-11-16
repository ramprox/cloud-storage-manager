package client.ui.model;

import interop.model.fileinfo.FileInfo;
import javafx.beans.property.SimpleObjectProperty;

public class FileInfoView implements Comparable<FileInfoView> {

    private final FileInfo fileInfo;

    public FileInfoView(FileInfo fileInfo) {
        this.fileInfo = fileInfo;
    }

    public SimpleObjectProperty<FileInfo> getFileInfoProperty() {
        return new SimpleObjectProperty<>(fileInfo);
    }

    public FileInfo getFileInfo() {
        return fileInfo;
    }

    @Override
    public int compareTo(FileInfoView other) {
        if (this.getFileInfo().equals(FileInfo.PARENT_DIR)) {
            return -1;
        } else if (other.getFileInfo().equals(FileInfo.PARENT_DIR)) {
            return 1;
        }
        return 0;
    }
}
