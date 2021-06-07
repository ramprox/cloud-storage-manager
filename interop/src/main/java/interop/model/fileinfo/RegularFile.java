package interop.model.fileinfo;

import java.io.Serializable;

/**
 * Класс несущий информацию о файле, который не является директорией
 */
public class RegularFile extends FileInfo implements Serializable {

    private long size;

    public RegularFile(String fileName, long lastModified, long size, long createDate) {
        super(fileName, lastModified, createDate);
        this.size = size;
    }

    public long getSize() {
        return size;
    }
}
