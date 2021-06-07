package interop.model.requests;

import interop.interfaces.FileNameReq;

import java.io.Serializable;

/**
 * Класс для запросов получения размера директории
 */
public class DirSizeReq implements FileNameReq, Serializable {

    private final String dirName;                         // название директории

    public DirSizeReq(String dirName) {
        this.dirName = dirName;
    }

    @Override
    public String getFileName() {
        return dirName;
    }
}
