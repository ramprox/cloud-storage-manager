package interop.model.requests.fileoperation;

import interop.interfaces.FileNameReq;

import java.io.Serializable;

/**
 * Абстрактный базовый класс для запросов файловых операций и вычисления размера директории
 */
public abstract class FileOperation implements Serializable, FileNameReq {
    private String fileName;

    protected FileOperation(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }
}
