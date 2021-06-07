package interop.model.responses.fileoperation;

import interop.model.fileinfo.FileInfo;

import java.io.Serializable;

/**
 * Класс для откликов клиенту по переименовыванию файлов
 */
public class RenameFileResp extends FileOperationResp implements Serializable {

    private FileInfo newFileInfo;

    public RenameFileResp(FileInfo oldFileName, FileInfo newFileName) {
        super(oldFileName);
        this.newFileInfo = newFileName;
    }

    public FileInfo getNewFileInfo() {
        return newFileInfo;
    }
}
