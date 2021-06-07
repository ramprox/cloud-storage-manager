package interop.model.fileinfo;

import java.io.Serializable;

/**
 * Абстрактный базовый класс для типов файлов
 */
public abstract class FileInfo implements Serializable {
    private final String fileName;
    private final long lastModified;
    private final long createDate;

    public FileInfo(String fileName, long lastModified, long createDate) {
        this.fileName = fileName;
        this.lastModified = lastModified;
        this.createDate = createDate;
    }

    public String getFileName() {
        return fileName;
    }

    public long getLastModified() {
        return lastModified;
    }

    public long getCreateDate() {
        return createDate;
    }
}
