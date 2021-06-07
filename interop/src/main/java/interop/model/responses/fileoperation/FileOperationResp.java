package interop.model.responses.fileoperation;

import interop.interfaces.FileInfoResp;
import interop.model.fileinfo.FileInfo;

import java.io.Serializable;

/**
 * Абстрактный базовый класс для откликов клиенту по удалению, созданию, переименовыванию файлов
 * и запросов размера директории
 */
public abstract class FileOperationResp implements Serializable, FileInfoResp {
    private FileInfo file;

    FileOperationResp(FileInfo file) {
        this.file = file;
    }

    public FileInfo getFile() {
        return file;
    }
}
