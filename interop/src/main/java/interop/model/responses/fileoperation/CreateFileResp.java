package interop.model.responses.fileoperation;

import interop.model.fileinfo.FileInfo;

import java.io.Serializable;

/**
 * Класс для откликов клиенту по созданию файлов
 */
public class CreateFileResp extends FileOperationResp implements Serializable {

    public CreateFileResp(FileInfo file) {
        super(file);
    }
}
