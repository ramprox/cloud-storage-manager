package client.ui.service;

import client.ui.model.FileInfoView;
import javafx.scene.control.TableColumn;
import org.springframework.stereotype.Component;

import java.util.Comparator;

@Component
public class FileInfoViewComparator {

    public Comparator<FileInfoView> byName(TableColumn.SortType sortType) {
        Comparator<FileInfoView> result = defaultCompare();
        if(sortType == TableColumn.SortType.ASCENDING) {
            return result.thenComparing(fileInfoView -> fileInfoView.getFileInfo().getFileName());
        }
        return result.thenComparing(Comparator.<FileInfoView, String>comparing(fileInfoView ->
                fileInfoView.getFileInfo().getFileName())
                .reversed());
    }

    public Comparator<FileInfoView> bySize(TableColumn.SortType sortType) {
        Comparator<FileInfoView> result = defaultCompare();
        if(sortType == TableColumn.SortType.ASCENDING) {
            return result.thenComparingLong(fileInfoView ->
                    fileInfoView.getFileInfo().getSize());
        }
        return result.thenComparing(Comparator.<FileInfoView>comparingLong(fileInfoView ->
                fileInfoView.getFileInfo().getSize())
                .reversed());
    }

    public Comparator<FileInfoView> byLastModifiedDate(TableColumn.SortType sortType) {
        Comparator<FileInfoView> result = defaultCompare();
        if(sortType == TableColumn.SortType.ASCENDING) {
            return result.thenComparingLong(fileInfoView ->
                    fileInfoView.getFileInfo().getLastModifiedDate());
        }
        return result.thenComparing(Comparator.<FileInfoView>comparingLong(fileInfoView ->
                fileInfoView.getFileInfo().getLastModifiedDate())
                .reversed());
    }

    public Comparator<FileInfoView> byCreateDate(TableColumn.SortType sortType) {
        Comparator<FileInfoView> result = defaultCompare();
        if(sortType == TableColumn.SortType.ASCENDING) {
            return result.thenComparingLong(fileInfoView ->
                    fileInfoView.getFileInfo().getCreationDate());
        }
        return result.thenComparing(Comparator.<FileInfoView>comparingLong(fileInfoView ->
                fileInfoView.getFileInfo().getCreationDate())
                .reversed());
    }

    private Comparator<FileInfoView> defaultCompare() {
        return Comparator.<FileInfoView>naturalOrder()
                .thenComparing(fileInfoView ->
                        fileInfoView.getFileInfo().getType());
    }
}
