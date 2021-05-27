package client.model;

import java.util.Date;

public class FileInfo implements Comparable<FileInfo> {

    private FileNameType fileNameType;
    private Long size;
    private Date lastModified;

    public FileInfo(FileNameType fileNameType, Long size, Date lastModified) {
        this.fileNameType = fileNameType;
        this.size = size;
        this.lastModified = lastModified;
    }

    public FileNameType getFileNameType() {
        return fileNameType;
    }

    public Long getSize() {
        return size;
    }

    public Date getLastModified() {
        return lastModified;
    }

    @Override
    public int compareTo(FileInfo other) {
        if(this.getFileNameType().getName().equals("[ . . ]")) {
            return -1;
        }
        if(other.getFileNameType().getName().equals("[ . . ]")) {
            return 1;
        }
        if(this.getFileNameType().getType().equals(FileType.Directory) &&
                other.getFileNameType().getType().equals(FileType.File)) {
            return -1;
        }
        if(this.getFileNameType().getType().equals(FileType.File) &&
                other.getFileNameType().getType().equals(FileType.Directory)) {
            return 1;
        }
        return this.getFileNameType().getName().compareTo(other.getFileNameType().getName());
    }
}
