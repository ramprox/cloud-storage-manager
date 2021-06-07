package client.utils;

import client.controllers.MainWindowController;
import client.model.FileInfoView;
import interop.model.fileinfo.FileType;
import javafx.scene.control.TableColumn;

import java.time.LocalDateTime;
import java.util.Comparator;

public class Comparators {

    public static Comparator<FileInfoView> comparatorByName(TableColumn.SortType sortType) {
        Comparator<FileInfoView> result = (fileInfoView1, fileInfoView2) -> {
            int compareByType = compareByType(fileInfoView1, fileInfoView2);
            if(compareByType != 0) {
                return compareByType;
            }
            if(sortType.equals(TableColumn.SortType.ASCENDING)) {
                return fileInfoView1.getName().compareTo(fileInfoView2.getName());
            } else {
                return fileInfoView2.getName().compareTo(fileInfoView1.getName());
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
            Long size1 = fileInfoView1.getSizeInLong();
            Long size2 = fileInfoView2.getSizeInLong();
            if(size1 == null || size2 == null) {
                return 0;
            }
            if(sortType.equals(TableColumn.SortType.ASCENDING)) {
                return fileInfoView1.getSizeInLong().compareTo(fileInfoView2.getSizeInLong());
            } else {
                return fileInfoView2.getSizeInLong().compareTo(fileInfoView1.getSizeInLong());
            }
        };
        return result;
    }

    public static Comparator<FileInfoView> comparatorByDate(TableColumn.SortType sortType) {
        Comparator<FileInfoView> result = (fileInfoView1, fileInfoView2) -> {
            int compareByType = compareByType(fileInfoView1, fileInfoView2);
            if(compareByType != 0) {
                return compareByType;
            }
            LocalDateTime dateTime1 = fileInfoView1.getLastModifiedInDate();
            LocalDateTime dateTime2 = fileInfoView2.getLastModifiedInDate();
            if(sortType.equals(TableColumn.SortType.ASCENDING)) {
                return dateTime1.compareTo(dateTime2);
            } else {
                return dateTime2.compareTo(dateTime1);
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
            LocalDateTime dateTime1 = fileInfoView1.getCreateDate();
            LocalDateTime dateTime2 = fileInfoView2.getCreateDate();
            if(sortType.equals(TableColumn.SortType.ASCENDING)) {
                return dateTime1.compareTo(dateTime2);
            } else {
                return dateTime2.compareTo(dateTime1);
            }
        };
        return result;
    }

    private static int compareByType(FileInfoView fileInfoView1, FileInfoView fileInfoView2) {
        if(fileInfoView1.getName().equals(MainWindowController.parentDir)) {
            return -1;
        }
        if(fileInfoView2.getName().equals(MainWindowController.parentDir)) {
            return 1;
        }
        if(fileInfoView1.getType().equals(FileType.DIR) &&
                fileInfoView2.getType().equals(FileType.FILE)) {
            return -1;
        }
        if(fileInfoView2.getType().equals(FileType.DIR) &&
                fileInfoView1.getType().equals(FileType.FILE)) {
            return 1;
        }
        return 0;
    }
}
