package client.model;

import interop.model.fileinfo.FileInfo;
import interop.model.fileinfo.FileType;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.TableColumn;

import java.util.Comparator;

public class FileInfoView implements Comparable<FileInfoView> {

    private FileInfo fileInfo;

    public FileInfoView(FileInfo fileInfo) {
        this.fileInfo = fileInfo;
    }

    public SimpleObjectProperty<FileInfo> getFileInfoProperty() {
        return new SimpleObjectProperty<>(fileInfo);
    }

    public FileInfo getFileInfo() {
        return fileInfo;
    }

    public static Comparator<FileInfoView> comparatorByName(TableColumn.SortType sortType) {
        Comparator<FileInfoView> result = (fileInfoView1, fileInfoView2) -> {
            int compareByType = compareByType(fileInfoView1, fileInfoView2);
            if(compareByType != 0) {
                return compareByType;
            }
            String filePath1 = fileInfoView1.getFileInfo().getFileName();
            String filePath2 = fileInfoView2.getFileInfo().getFileName();
            if(sortType.equals(TableColumn.SortType.ASCENDING)) {
                return filePath1.compareTo(filePath2);
            } else {
                return filePath2.compareTo(filePath1);
            }
        };
        return result;
    }

    public static Comparator<FileInfoView> comparatorBySize(TableColumn.SortType sortType) {
        Comparator<FileInfoView> result = (fileInfoView1, fileInfoView2) -> {
            int compareByType = compareByType(fileInfoView1, fileInfoView2);
            if(compareByType != 0) {
                return compareByType;
            }
            long size1 = fileInfoView1.getFileInfo().getSize();
            long size2 = fileInfoView2.getFileInfo().getSize();
            if(sortType.equals(TableColumn.SortType.ASCENDING)) {
                return Long.compare(size1, size2);
            } else {
                return Long.compare(size2, size1);
            }
        };
        return result;
    }

    public static Comparator<FileInfoView> comparatorByLastModifiedDate(TableColumn.SortType sortType) {
        Comparator<FileInfoView> result = (fileInfoView1, fileInfoView2) -> {
            int compareByType = compareByType(fileInfoView1, fileInfoView2);
            if(compareByType != 0) {
                return compareByType;
            }
            long dateTime1 = fileInfoView1.getFileInfo().getLastModifiedDate();
            long dateTime2 = fileInfoView2.getFileInfo().getLastModifiedDate();
            if(sortType.equals(TableColumn.SortType.ASCENDING)) {
                return Long.compare(dateTime1, dateTime2);
            } else {
                return Long.compare(dateTime2, dateTime1);
            }
        };
        return result;
    }

    public static Comparator<FileInfoView> comparatorByCreateDate(TableColumn.SortType sortType) {
        Comparator<FileInfoView> result = (fileInfoView1, fileInfoView2) -> {
            int compareByType = compareByType(fileInfoView1, fileInfoView2);
            if(compareByType != 0) {
                return compareByType;
            }
            long dateTime1 = fileInfoView1.getFileInfo().getCreationDate();
            long dateTime2 = fileInfoView2.getFileInfo().getCreationDate();
            if(sortType.equals(TableColumn.SortType.ASCENDING)) {
                return Long.compare(dateTime1, dateTime2);
            } else {
                return Long.compare(dateTime2, dateTime1);
            }
        };
        return result;
    }

    private static int compareByType(FileInfoView fileInfoView1, FileInfoView fileInfoView2) {
        FileInfo fileInfo1 = fileInfoView1.getFileInfo();
        FileInfo fileInfo2 = fileInfoView2.getFileInfo();
        if(fileInfo1.equals(FileInfo.PARENT_DIR)) {
            return -1;
        }
        if(fileInfo2.equals(FileInfo.PARENT_DIR)) {
            return 1;
        }
        FileType type1 = fileInfoView1.getFileInfo().getType();
        FileType type2 = fileInfoView2.getFileInfo().getType();
        if(type1 == FileType.DIR && type2 == FileType.FILE) {
            return -1;
        }
        if(type2 == FileType.DIR && type1 == FileType.FILE) {
            return 1;
        }
        return 0;
    }

    @Override
    public int compareTo(FileInfoView other) {
        FileInfo fileInfo1 = this.getFileInfo();
        FileInfo fileInfo2 = other.getFileInfo();
        if(fileInfo1.equals(FileInfo.PARENT_DIR)) {
            return -1;
        }
        if(fileInfo2.equals(FileInfo.PARENT_DIR)) {
            return 1;
        }
        FileType type1 = this.getFileInfo().getType();
        FileType type2 = other.getFileInfo().getType();
        if(type1 == FileType.DIR && type2 == FileType.FILE) {
            return -1;
        }
        if(type2 == FileType.DIR && type1 == FileType.FILE) {
            return 1;
        }
        return 0;
    }
}
