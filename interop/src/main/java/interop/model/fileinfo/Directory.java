package interop.model.fileinfo;

import java.io.Serializable;

/**
 * Класс несущий информацию о директории
 */
public class Directory extends FileInfo implements Serializable {
    public Directory(String fileName, long lastModified, long createDate) {
        super(fileName, lastModified, createDate);
    }
}
