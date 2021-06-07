package interop.model.responses.fileoperation;

import interop.model.fileinfo.FileInfo;

import java.io.Serializable;

/**
 * Класс для откликов клиенту по удалению файлов
 */
public class DeleteFileResp extends FileOperationResp implements Serializable {

    public DeleteFileResp(FileInfo file) {
        super(file);
    }
}
