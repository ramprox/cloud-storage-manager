package client.model;

import interop.model.fileinfo.FileNameType;
import interop.model.fileinfo.FileType;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Модель данных для представления в таблице
 */
public class FileInfoView {

    private static final String pattern = "dd-MM-yyyy HH:mm";

    private FileNameType fileNameType;
    private Long size;
    private String lastModified;
    private String fileCreateDate;

    public SimpleStringProperty getFileCreateDate() {
        return new SimpleStringProperty(fileCreateDate);
    }

    public SimpleStringProperty getLastModified() {
        return new SimpleStringProperty(lastModified);
    }

    public SimpleLongProperty getSize() {
        return new SimpleLongProperty(size == null ? -1 : size);
    }

    public void setSize(long size) {
        this.size = size;
    }


    public FileInfoView(FileNameType fileNameType, Long size, Long lastModified, Long createDate) {
        this.fileNameType = fileNameType;
        this.size = size;
        String strDate = null;
        if(lastModified != null) {
            LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(lastModified), ZoneId.systemDefault());
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            strDate = dateTime.format(formatter);
            this.lastModified = strDate;
        }
        this.lastModified = strDate;
        String strCreateDate = null;
        if(createDate != null) {
            LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(createDate), ZoneId.systemDefault());
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            strCreateDate = dateTime.format(formatter);
            this.fileCreateDate = strCreateDate;
        }
        this.fileCreateDate = strCreateDate;
    }

    public String getName() {
        return fileNameType.getFileName();
    }

    public void setName(String name) {
        fileNameType.setFileName(name);
    }

    public FileType getType() {
        return fileNameType.getFileType();
    }

    public FileNameType getFileNameType() {
        return fileNameType;
    }

    public Long getSizeInLong() {
        return size;
    }

    public LocalDateTime getLastModifiedInDate() {
        if(lastModified == null) {
            return null;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        LocalDateTime localDateTime = LocalDateTime.parse(lastModified, formatter);
        return localDateTime;
    }

    public LocalDateTime getCreateDate() {
        if(fileCreateDate == null) {
            return null;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        LocalDateTime localDateTime = LocalDateTime.parse(fileCreateDate, formatter);
        return localDateTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FileInfoView that = (FileInfoView) o;

        return fileNameType != null ? fileNameType.equals(that.fileNameType) : that.fileNameType == null;
    }

    @Override
    public int hashCode() {
        return fileNameType != null ? fileNameType.hashCode() : 0;
    }
}
