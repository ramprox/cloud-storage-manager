package interop.model.fileinfo;

import java.io.Serializable;

public class FileInfo implements Serializable {
    public static final String PARENT_DIR_NAME = "..";         // строка для отображения родительской директории
    private FileType type;
    private String fileName;
    private long size;
    private long lastModifiedDate;
    private long creationDate;

    public FileInfo(FileType type, String fileName, long size, long lastModifiedDate, long creationDate) {
        this.type = type;
        this.fileName = fileName;
        this.size = size;
        this.lastModifiedDate = lastModifiedDate;
        this.creationDate = creationDate;
    }

    public FileType getType() {
        return type;
    }

    public String getFileName() {
        return fileName;
    }

    public long getSize() {
        return size;
    }

    public long getLastModifiedDate() {
        return lastModifiedDate;
    }

    public long getCreationDate() {
        return creationDate;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public void setFileName(String filePath) {
        this.fileName = filePath;
    }

    public void setLastModifiedDate(long lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public void setCreationDate(long creationDate) {
        this.creationDate = creationDate;
    }

    public static final FileInfo PARENT_DIR = new FileInfo(FileType.DIR, PARENT_DIR_NAME,
            -1L, -1L, -1L);

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileInfo fileInfo1 = (FileInfo) o;
        return fileName != null ? fileName.equals(fileInfo1.fileName) : fileInfo1.fileName == null;
    }

    @Override
    public int hashCode() {
        return fileName != null ? fileName.hashCode() : 0;
    }
}
