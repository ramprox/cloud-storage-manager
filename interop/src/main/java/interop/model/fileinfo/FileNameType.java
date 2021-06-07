package interop.model.fileinfo;

import java.io.Serializable;

/**
 * Класс несущий информацию об имени и типе файла
 */
public class FileNameType implements Serializable {
    private String fileName;
    private FileType fileType;

    public FileNameType(String fileName, FileType fileType) {
        this.fileName = fileName;
        this.fileType = fileType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public FileType getFileType() {
        return fileType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FileNameType that = (FileNameType) o;

        if (fileName != null ? !fileName.equals(that.fileName) : that.fileName != null) return false;
        return fileType == that.fileType;
    }

    @Override
    public int hashCode() {
        int result = fileName != null ? fileName.hashCode() : 0;
        result = 31 * result + (fileType != null ? fileType.hashCode() : 0);
        return result;
    }
}
